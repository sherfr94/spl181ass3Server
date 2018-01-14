package bgu.spl181.net.api.json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class User implements Serializable
{
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance="0";
        this.country="";
        this.movies=new ArrayList<>();
        this.type="normal";

    }

    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("country")
    @Expose
    private String country;
    @SerializedName("movies")
    @Expose
    private List<UserMovie> movies = null;
    @SerializedName("balance")
    @Expose
    private String balance;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public List<UserMovie> getMovies() {
        return movies;
    }

    public void setMovies(List<UserMovie> movies) {
        this.movies = movies;
    }

    public Integer getBalance() {
        return Integer.parseInt(balance);
    }

    public void setBalance(Integer balance) {
        this.balance = ""+balance;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", type='" + type + '\'' +
                ", password='" + password + '\'' +
                ", country='" + country + '\'' +
                ", movies=" + movies +
                ", balance=" + balance +
                '}';
    }
}