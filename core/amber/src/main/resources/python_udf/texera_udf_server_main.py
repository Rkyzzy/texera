import ast
import importlib.util
import json
import logging
import os
import sys
import threading
import traceback
from datetime import datetime
from pathlib import Path
from typing import Dict

import pandas
import pyarrow
import pyarrow.flight
from pyarrow._flight import FlightDescriptor, Action

import texera_udf_operator_base


class UDFServer(pyarrow.flight.FlightServerBase):
    logger = logging.getLogger("PythonUDF.pyarrow_flight_server")

    def __init__(self, udf_op, host: str = "localhost", location=None, tls_certificates=None, auth_handler=None):
        super(UDFServer, self).__init__(location, auth_handler, tls_certificates)
        self.flights: Dict = {}
        self.host: str = host
        self.tls_certificates = tls_certificates
        self.udf_op = udf_op

    @classmethod
    def _descriptor_to_key(cls, descriptor: FlightDescriptor):
        return descriptor.descriptor_type.value, descriptor.command, tuple(descriptor.path or tuple())

    def _make_flight_info(self, key, descriptor: FlightDescriptor, table):
        """NOT USED NOW"""
        if self.tls_certificates:
            location = pyarrow.flight.Location.for_grpc_tls(self.host, self.port)
        else:
            location = pyarrow.flight.Location.for_grpc_tcp(self.host, self.port)
        endpoints = [pyarrow.flight.FlightEndpoint(repr(key), [location]), ]

        mock_sink = pyarrow.MockOutputStream()
        stream_writer = pyarrow.RecordBatchStreamWriter(mock_sink, table.schema)
        stream_writer.write_table(table)
        stream_writer.close()
        data_size = mock_sink.size()

        return pyarrow.flight.FlightInfo(table.schema,
                                         descriptor, endpoints,
                                         table.num_rows, data_size)

    def list_flights(self, context, criteria):
        """

        NOT USED NOW

        Getting a list of available datasets on the server. This method is not used here,
        but might be useful in the future.
        """
        for key, table in self.flights.items():
            if key[1] is not None:
                descriptor = pyarrow.flight.FlightDescriptor.for_command(key[1])
            else:
                descriptor = pyarrow.flight.FlightDescriptor.for_path(*key[2])

            yield self._make_flight_info(key, descriptor, table)

    def get_flight_info(self, context, descriptor: FlightDescriptor):
        """

        NOT USED NOW

        Returning an “access plan” for a dataset of interest, possibly requiring consuming multiple data streams.
        This request can accept custom serialized commands containing, for example, your specific
        application parameters.
        """
        key = UDFServer._descriptor_to_key(descriptor)
        if key in self.flights:
            table = self.flights[key]
            return self._make_flight_info(key, descriptor, table)
        raise KeyError('Flight not found.')

    def do_put(self, context, descriptor: FlightDescriptor, reader, writer):
        """
        Pass Arrow stream from the client to the server. The data must be associated with a `FlightDescriptor`,
        which can be either a path or a command. Here the path is not actually a path on the disk,
        but rather an identifier.
        """
        self.flights[UDFServer._descriptor_to_key(descriptor)] = reader.read_all()

    def do_get(self, context, ticket):
        """
        Before getting the stream, the client must first ask the server for available tickets
        (to the specified dataset) of the specified `FlightDescriptor`.
        """
        key = ast.literal_eval(ticket.ticket.decode())
        if key not in self.flights:
            logger.warning("Flight Server:\tNOT IN")
            return None
        return pyarrow.flight.RecordBatchStream(self.flights[key])

    def do_action(self, context, action: Action):
        """
        Each (implementation-specific) action is a string (defined in the script). The client is expected to know
        available actions. When a specific action is called, the server executes the corresponding action and
        maybe will return any results, i.e. a generalized function call.
        """
        self.logger.debug(f"Flight Server on Action {action.type}")
        if action.type == "health_check":
            # to check the status of the server to see if it is running.
            yield self._response(b'Flight Server is up and running!')
        elif action.type == "open":

            # set up user configurations
            user_conf_table = self.flights[self._descriptor_to_key(self._to_descriptor(b'conf'))]
            self._configure(*user_conf_table.to_pydict()['conf'])

            # open UDF
            user_args_table = self.flights[self._descriptor_to_key(self._to_descriptor(b'args'))]
            self.udf_op.open(*user_args_table.to_pydict()['args'])

            yield self._response(b'Success!')
        elif action.type == "compute":
            # execute UDF
            # prepare input data

            try:
                input_dataframe: pandas.DataFrame = self._get_flight("toPython")

                # execute and output data
                for index, row in input_dataframe.iterrows():
                    self.udf_op.accept(row)
                self._output_data()
                result_buffer = json.dumps({'status': 'Success'})
            except:
                result_buffer = json.dumps({'status': 'Fail', 'errorMessage': traceback.format_exc()})

            # discard this batch of input
            self._remove_flight("toPython")

            yield self._response(result_buffer.encode('utf-8'))

        elif action.type == "input_exhausted":
            self.udf_op.input_exhausted()
            self._output_data()
            yield self._response(b'Success!')

        elif action.type == "close":
            # close UDF
            self.udf_op.close()
            yield self._response(b'Success!')

        elif action.type == "terminate":
            # Shut down on background thread to avoid blocking current request
            # this is to be invoked by java end whenever it needs to terminate the server on python end
            threading.Thread(target=self._delayed_shutdown).start()

        else:
            raise ValueError("Unknown action {!r}".format(action.type))

    def _delayed_shutdown(self):
        """Shut down after a delay."""
        self.logger.debug("Bye bye!")
        self.shutdown()
        self.wait()

    def _output_data(self):
        output_data_list = []
        while self.udf_op.has_next():
            output_data_list.append(self.udf_op.next())
        output_dataframe = pandas.DataFrame.from_records(output_data_list)
        # send output data to Java
        output_key = self._descriptor_to_key(self._to_descriptor(b'fromPython'))
        self.flights[output_key] = pyarrow.Table.from_pandas(output_dataframe)

    def _get_flight(self, channel: str) -> pandas.DataFrame:
        self.logger.debug(f"transforming flight {channel.__repr__()}")
        df = self.flights[self._descriptor_to_key(self._to_descriptor(channel.encode()))].to_pandas()
        self.logger.debug(f"got {len(df)} rows in this flight")
        return df

    def _remove_flight(self, channel: str) -> None:
        self.logger.debug(f"removing flight {channel.__repr__()}")
        self.flights.pop(self._descriptor_to_key(self._to_descriptor(channel.encode())))

    @staticmethod
    def _response(message: bytes):
        return pyarrow.flight.Result(pyarrow.py_buffer(message))

    @staticmethod
    def _to_descriptor(channel: bytes) -> FlightDescriptor:
        return pyarrow.flight.FlightDescriptor.for_path(channel)

    def _configure(self, *args):
        self._setup_logger(*args)

    def _setup_logger(self, *args):
        # TODO: make it kwargs
        # create a file handler

        log_dir = args[0]
        file_name = f"{datetime.utcnow().isoformat()}-{os.getpid()}"
        file_path = Path(log_dir).joinpath(file_name)
        file_handler = logging.FileHandler(file_path)

        formatter = logging.Formatter("%(levelname)s: %(asctime)s - %(name)s - %(process)s - %(message)s")

        # hacky way to parse for log level
        log_level = eval(f"logging.{args[1]}")

        file_handler.setLevel(log_level)
        file_handler.setFormatter(formatter)
        logger.info(f"Attaching a FileHandler to logger, file path: {file_path}")
        logger.addHandler(file_handler)
        logger.info(f"Logger FileHandler is now attached, previous logs are in StreamHandler only.")


if __name__ == '__main__':

    # configure root logger
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    stream_handler = logging.StreamHandler()
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    stream_handler.setLevel(logging.INFO)
    stream_handler.setFormatter(formatter)
    logger.addHandler(stream_handler)

    _, port, UDF_operator_script_path, *__ = sys.argv
    # Dynamically import operator from user-defined script.

    # Spec is used to load a spec based on a file location (the UDF script)
    spec = importlib.util.spec_from_file_location('user_module', UDF_operator_script_path)
    # Dynamically load the user script as module
    user_module = importlib.util.module_from_spec(spec)
    # Execute the module so that its attributes can be loaded.
    spec.loader.exec_module(user_module)

    # The UDF that will be used in the server. It will be either an inherited operator instance, or created by passing
    # map_func/filter_func to a TexeraMapOperator/TexeraFilterOperator instance.
    final_UDF = None

    if hasattr(user_module, 'operator_instance'):
        final_UDF = user_module.operator_instance
    elif hasattr(user_module, 'map_function'):
        final_UDF = texera_udf_operator_base.TexeraMapOperator(user_module.map_function)
    elif hasattr(user_module, 'filter_function'):
        final_UDF = texera_udf_operator_base.TexeraFilterOperator(user_module.filter_function)
    else:
        raise ValueError("Unsupported UDF definition!")

    location = "grpc+tcp://localhost:" + port

    UDFServer(final_UDF, "localhost", location).serve()
