/*
 * This file is generated by jOOQ.
 */
package edu.uci.ics.texera.web.model.jooq.generated.tables.pojos;


import edu.uci.ics.texera.web.model.jooq.generated.tables.interfaces.IFile;

import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class File implements IFile {

    private static final long serialVersionUID = 963831931;

    private UInteger uid;
    private UInteger fid;
    private UInteger size;
    private String   name;
    private String   path;
    private String   description;

    public File() {}

    public File(IFile value) {
        this.uid = value.getUid();
        this.fid = value.getFid();
        this.size = value.getSize();
        this.name = value.getName();
        this.path = value.getPath();
        this.description = value.getDescription();
    }

    public File(
        UInteger uid,
        UInteger fid,
        UInteger size,
        String   name,
        String   path,
        String   description
    ) {
        this.uid = uid;
        this.fid = fid;
        this.size = size;
        this.name = name;
        this.path = path;
        this.description = description;
    }

    @Override
    public UInteger getUid() {
        return this.uid;
    }

    @Override
    public void setUid(UInteger uid) {
        this.uid = uid;
    }

    @Override
    public UInteger getFid() {
        return this.fid;
    }

    @Override
    public void setFid(UInteger fid) {
        this.fid = fid;
    }

    @Override
    public UInteger getSize() {
        return this.size;
    }

    @Override
    public void setSize(UInteger size) {
        this.size = size;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("File (");

        sb.append(uid);
        sb.append(", ").append(fid);
        sb.append(", ").append(size);
        sb.append(", ").append(name);
        sb.append(", ").append(path);
        sb.append(", ").append(description);

        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IFile from) {
        setUid(from.getUid());
        setFid(from.getFid());
        setSize(from.getSize());
        setName(from.getName());
        setPath(from.getPath());
        setDescription(from.getDescription());
    }

    @Override
    public <E extends IFile> E into(E into) {
        into.from(this);
        return into;
    }
}
