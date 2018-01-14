package bgu.spl181.net.api.json;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class UserMovie implements Serializable
{
    public UserMovie() {}


    public UserMovie(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @SerializedName("id")
    @Expose
    protected String id;
    @SerializedName("name")
    @Expose
    protected String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}