/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated;


import edu.uci.ics.texera.web.model.jooq.generated.tables.File;
import edu.uci.ics.texera.web.model.jooq.generated.tables.KeywordDictionary;
import edu.uci.ics.texera.web.model.jooq.generated.tables.User;
import edu.uci.ics.texera.web.model.jooq.generated.tables.UserDictionary;
import edu.uci.ics.texera.web.model.jooq.generated.tables.Workflow;
import edu.uci.ics.texera.web.model.jooq.generated.tables.WorkflowOfUser;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.FileRecord;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.KeywordDictionaryRecord;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.UserDictionaryRecord;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.UserRecord;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.WorkflowOfUserRecord;
import edu.uci.ics.texera.web.model.jooq.generated.tables.records.WorkflowRecord;

import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;
import org.jooq.types.UInteger;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>texera_db</code> schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<FileRecord, UInteger> IDENTITY_FILE = Identities0.IDENTITY_FILE;
    public static final Identity<KeywordDictionaryRecord, UInteger> IDENTITY_KEYWORD_DICTIONARY = Identities0.IDENTITY_KEYWORD_DICTIONARY;
    public static final Identity<UserRecord, UInteger> IDENTITY_USER = Identities0.IDENTITY_USER;
    public static final Identity<WorkflowRecord, UInteger> IDENTITY_WORKFLOW = Identities0.IDENTITY_WORKFLOW;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<FileRecord> KEY_FILE_UID = UniqueKeys0.KEY_FILE_UID;
    public static final UniqueKey<FileRecord> KEY_FILE_PRIMARY = UniqueKeys0.KEY_FILE_PRIMARY;
    public static final UniqueKey<KeywordDictionaryRecord> KEY_KEYWORD_DICTIONARY_UID = UniqueKeys0.KEY_KEYWORD_DICTIONARY_UID;
    public static final UniqueKey<KeywordDictionaryRecord> KEY_KEYWORD_DICTIONARY_PRIMARY = UniqueKeys0.KEY_KEYWORD_DICTIONARY_PRIMARY;
    public static final UniqueKey<UserRecord> KEY_USER_PRIMARY = UniqueKeys0.KEY_USER_PRIMARY;
    public static final UniqueKey<UserRecord> KEY_USER_GOOGLE_ID = UniqueKeys0.KEY_USER_GOOGLE_ID;
    public static final UniqueKey<UserDictionaryRecord> KEY_USER_DICTIONARY_PRIMARY = UniqueKeys0.KEY_USER_DICTIONARY_PRIMARY;
    public static final UniqueKey<WorkflowRecord> KEY_WORKFLOW_PRIMARY = UniqueKeys0.KEY_WORKFLOW_PRIMARY;
    public static final UniqueKey<WorkflowOfUserRecord> KEY_WORKFLOW_OF_USER_PRIMARY = UniqueKeys0.KEY_WORKFLOW_OF_USER_PRIMARY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<FileRecord, UserRecord> FILE_IBFK_1 = ForeignKeys0.FILE_IBFK_1;
    public static final ForeignKey<KeywordDictionaryRecord, UserRecord> KEYWORD_DICTIONARY_IBFK_1 = ForeignKeys0.KEYWORD_DICTIONARY_IBFK_1;
    public static final ForeignKey<UserDictionaryRecord, UserRecord> USER_DICTIONARY_IBFK_1 = ForeignKeys0.USER_DICTIONARY_IBFK_1;
    public static final ForeignKey<WorkflowOfUserRecord, UserRecord> WORKFLOW_OF_USER_IBFK_1 = ForeignKeys0.WORKFLOW_OF_USER_IBFK_1;
    public static final ForeignKey<WorkflowOfUserRecord, WorkflowRecord> WORKFLOW_OF_USER_IBFK_2 = ForeignKeys0.WORKFLOW_OF_USER_IBFK_2;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 {
        public static Identity<FileRecord, UInteger> IDENTITY_FILE = Internal.createIdentity(File.FILE, File.FILE.FID);
        public static Identity<KeywordDictionaryRecord, UInteger> IDENTITY_KEYWORD_DICTIONARY = Internal.createIdentity(KeywordDictionary.KEYWORD_DICTIONARY, KeywordDictionary.KEYWORD_DICTIONARY.KID);
        public static Identity<UserRecord, UInteger> IDENTITY_USER = Internal.createIdentity(User.USER, User.USER.UID);
        public static Identity<WorkflowRecord, UInteger> IDENTITY_WORKFLOW = Internal.createIdentity(Workflow.WORKFLOW, Workflow.WORKFLOW.WID);
    }

    private static class UniqueKeys0 {
        public static final UniqueKey<FileRecord> KEY_FILE_UID = Internal.createUniqueKey(File.FILE, "KEY_file_uid", File.FILE.UID, File.FILE.NAME);
        public static final UniqueKey<FileRecord> KEY_FILE_PRIMARY = Internal.createUniqueKey(File.FILE, "KEY_file_PRIMARY", File.FILE.FID);
        public static final UniqueKey<KeywordDictionaryRecord> KEY_KEYWORD_DICTIONARY_UID = Internal.createUniqueKey(KeywordDictionary.KEYWORD_DICTIONARY, "KEY_keyword_dictionary_uid", KeywordDictionary.KEYWORD_DICTIONARY.UID, KeywordDictionary.KEYWORD_DICTIONARY.NAME);
        public static final UniqueKey<KeywordDictionaryRecord> KEY_KEYWORD_DICTIONARY_PRIMARY = Internal.createUniqueKey(KeywordDictionary.KEYWORD_DICTIONARY, "KEY_keyword_dictionary_PRIMARY", KeywordDictionary.KEYWORD_DICTIONARY.KID);
        public static final UniqueKey<UserRecord> KEY_USER_PRIMARY = Internal.createUniqueKey(User.USER, "KEY_user_PRIMARY", User.USER.UID);
        public static final UniqueKey<UserRecord> KEY_USER_GOOGLE_ID = Internal.createUniqueKey(User.USER, "KEY_user_google_id", User.USER.GOOGLE_ID);
        public static final UniqueKey<UserDictionaryRecord> KEY_USER_DICTIONARY_PRIMARY = Internal.createUniqueKey(UserDictionary.USER_DICTIONARY, "KEY_user_dictionary_PRIMARY", UserDictionary.USER_DICTIONARY.UID, UserDictionary.USER_DICTIONARY.KEY);
        public static final UniqueKey<WorkflowRecord> KEY_WORKFLOW_PRIMARY = Internal.createUniqueKey(Workflow.WORKFLOW, "KEY_workflow_PRIMARY", Workflow.WORKFLOW.WID);
        public static final UniqueKey<WorkflowOfUserRecord> KEY_WORKFLOW_OF_USER_PRIMARY = Internal.createUniqueKey(WorkflowOfUser.WORKFLOW_OF_USER, "KEY_workflow_of_user_PRIMARY", WorkflowOfUser.WORKFLOW_OF_USER.UID, WorkflowOfUser.WORKFLOW_OF_USER.WID);
    }

    private static class ForeignKeys0 {
        public static final ForeignKey<FileRecord, UserRecord> FILE_IBFK_1 = Internal.createForeignKey(edu.uci.ics.texera.web.model.jooq.generated.Keys.KEY_USER_PRIMARY, File.FILE, "file_ibfk_1", File.FILE.UID);
        public static final ForeignKey<KeywordDictionaryRecord, UserRecord> KEYWORD_DICTIONARY_IBFK_1 = Internal.createForeignKey(edu.uci.ics.texera.web.model.jooq.generated.Keys.KEY_USER_PRIMARY, KeywordDictionary.KEYWORD_DICTIONARY, "keyword_dictionary_ibfk_1", KeywordDictionary.KEYWORD_DICTIONARY.UID);
        public static final ForeignKey<UserDictionaryRecord, UserRecord> USER_DICTIONARY_IBFK_1 = Internal.createForeignKey(edu.uci.ics.texera.web.model.jooq.generated.Keys.KEY_USER_PRIMARY, UserDictionary.USER_DICTIONARY, "user_dictionary_ibfk_1", UserDictionary.USER_DICTIONARY.UID);
        public static final ForeignKey<WorkflowOfUserRecord, UserRecord> WORKFLOW_OF_USER_IBFK_1 = Internal.createForeignKey(edu.uci.ics.texera.web.model.jooq.generated.Keys.KEY_USER_PRIMARY, WorkflowOfUser.WORKFLOW_OF_USER, "workflow_of_user_ibfk_1", WorkflowOfUser.WORKFLOW_OF_USER.UID);
        public static final ForeignKey<WorkflowOfUserRecord, WorkflowRecord> WORKFLOW_OF_USER_IBFK_2 = Internal.createForeignKey(edu.uci.ics.texera.web.model.jooq.generated.Keys.KEY_WORKFLOW_PRIMARY, WorkflowOfUser.WORKFLOW_OF_USER, "workflow_of_user_ibfk_2", WorkflowOfUser.WORKFLOW_OF_USER.WID);
    }
}
