package bgu.spl181.net.api.bidi;

import bgu.spl181.net.api.json.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MovieMessagingProtocol<T> extends UserMessagingProtocol<T>{

    protected MoviesList moviesList;
    protected ArrayList<Movie> movies;
    protected ConcurrentHashMap<String,Movie> moviesInfo;

    protected Integer maxMovieId = 0;


    public MovieMessagingProtocol(UsersList users, MoviesList movies){
        super(users);
        this.moviesList = movies;
        this.movies= (ArrayList) movies.getMovies();
        this.moviesInfo = new ConcurrentHashMap<>();

        for(Movie movie : this.movies){
            moviesInfo.put(movie.getName(),movie);
            Integer intId = Integer.parseInt(movie.getId());
            if(intId>maxMovieId) maxMovieId=intId;
        }
    }

    protected synchronized void updateMoviesJSON() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter("Database/Movies.json");
        writer.write(gson.toJson(moviesList));
        writer.close();
    }

    //Register to movie service must have country!
    @Override
    protected void register(String str) throws IOException {

        if(!str.contains("country=\"")){

            connections.send(connectionId,"ERROR register failed");
            return;
        }
        else{
            super.register(str);
        }
    }

    protected void balanceInfo(){
        String username = this.loggedIn.get(connectionId);
        Integer balance = this.usersInfo.get(username).getBalance();
        connections.send(connectionId,"ACK balance "+balance);

    }

    protected void balanceAdd(String str) throws IOException {
        String username = this.loggedIn.get(connectionId);
        str = str.substring(str.lastIndexOf(" ")+1);
        Integer amount = Integer.parseInt(str);
        Integer balance = usersInfo.get(username).getBalance();
        usersInfo.get(username).setBalance(balance+amount);
        updateUsersJSON();
        connections.send(connectionId,"ACK balance "+(balance+amount)+" added "+amount);
    }

    protected void info(){
        String result = "ACK info";
        for(Movie movie : movies){
            result=result+" \""+movie.getName()+"\"";
        }
        connections.send(connectionId,result);
    }

    protected void infoMovie(String str){
        int pos1 = str.indexOf("\"");
        int pos2 = str.lastIndexOf("\"");
        String moviename= str.substring(pos1+1, pos2);

        if(!moviesInfo.containsKey(moviename)){
            connections.send(connectionId,"ERROR request info failed");
            return;
        }

        connections.send(connectionId,"ACK info "+moviesInfo.get(moviename).info());
    }

    public MoviesList getMoviesList() {
        return moviesList;
    }

    @Override
    public BidiMessagingProtocol<T> get() {
        MovieMessagingProtocol mmp = new MovieMessagingProtocol(usersList, moviesList);
        mmp.start(connectionId, connections);
        return mmp;
    }

    protected void rentMovie(String str) throws IOException {
        String username = this.loggedIn.get(connectionId);
        String country = usersInfo.get(username).getCountry();
        boolean error=false;

        int pos1 = str.indexOf("\"");
        int pos2 = str.lastIndexOf("\"");
        String moviename= str.substring(pos1+1, pos2);

        //2 - movie doesn't exist
        if(!moviesInfo.containsKey(moviename)){
            error=true;
        }
        //4 - user is in banned country of movie
        else if(moviesInfo.get(moviename).getBannedCountries().contains(country)){
            error=true;
        }
        //3
       else if(moviesInfo.get(moviename).getAvailableAmount()==0){
             error=true;
        }
        //5 - user already rents the movie
        else{
            ArrayList<UserMovie> umlist = (ArrayList) usersInfo.get(username).getMovies();
            for (UserMovie um : umlist) {
                if (um.getName().equals(moviename)) error = true;
                break;

            }
        }
            //1 - you don't have enough money
            if(!error){
                Integer moviePrice = moviesInfo.get(moviename).getPrice();
                Integer userBalance = usersInfo.get(username).getBalance();

                if(moviePrice>userBalance){

                    error = true;
                }
            }


        if(error){
            connections.send(connectionId,"ERROR request rent failed");
        }
        else{
            //reduce availabe amount
            Integer copies = moviesInfo.get(moviename).getAvailableAmount();

            synchronized (lock){
                moviesInfo.get(moviename).setAvailableAmount(copies-1);
                updateMoviesJSON();
            }


            //remove balance by cost
            Integer moviePrice = moviesInfo.get(moviename).getPrice();
            Integer userBalance = usersInfo.get(username).getBalance();

            usersInfo.get(username).setBalance(userBalance-moviePrice);

            //add to user movie list
            usersInfo.get(username).getMovies().add(new UserMovie(moviesInfo.get(moviename).getId(), moviename));
            updateUsersJSON();

            connections.send(connectionId,"ACK rent \""+moviename+"\" success");
            connections.broadcast("BROADCAST movie \""+moviename+"\" "+(copies-1)+" "+moviePrice);
        }

    }

    protected void returnMovie(String str) throws IOException {
        String username = this.loggedIn.get(connectionId);
        int pos1 = str.indexOf("\"");
        int pos2 = str.lastIndexOf("\"");
        String moviename= str.substring(pos1+1, pos2);

        UserMovie toRemove = new UserMovie();
        boolean error=false;

        //3 - movie doesn't exist
        if(!moviesInfo.containsKey(moviename)) error=true;
            //2 - user not renting
        else {
            boolean found=false;
            for(UserMovie movie: usersInfo.get(username).getMovies()){
                if(movie.getName().equals(moviename)){
                    found=true;
                    toRemove=movie;
                    break;
                }
            }
            if(!found) error=true;
        }

        if(error){
            connections.send(connectionId, "ERROR request return failed");
            return;
        }
        else{
            Integer moviePrice = moviesInfo.get(moviename).getPrice();
            Integer copies = moviesInfo.get(moviename).getAvailableAmount();

            synchronized (lock){
                usersInfo.get(username).getMovies().remove(toRemove);
                updateUsersJSON();
                moviesInfo.get(moviename).setAvailableAmount(copies+1);
                updateMoviesJSON();
            }

            connections.send(connectionId,"ACK return \""+moviename+"\" success");
            connections.broadcast("BROADCAST movie \""+moviename+"\" "+(copies+1)+" "+moviePrice);
        }
    }



    //admin

    protected void addMovie(String str) throws IOException {
        int pos3 = str.indexOf("\"");
        String moviename=str.substring(0,pos3);
        //2 - movie already exists
        if(moviesInfo.containsKey(moviename)){
            connections.send(connectionId,"ERROR request addmovie failed");
            return;
        }

        else{

            str=str.substring(pos3+2);

            int pos4=str.indexOf(" ");
            String amount = str.substring(0,pos4);
            str=str.substring(pos4+1);

            pos4 = str.indexOf(" ");
            String price;
            ArrayList<String> bannedcountries = new ArrayList<>();

            if(pos4==-1){
                price = str;
            }
            else{//yes banned country
                price=str.substring(0,pos4);
                str=str.substring(pos4+1);
                int pos = str.indexOf(" ");
                while(pos!=-1){
                    int start = str.indexOf("\"");
                    int end = str.indexOf("\"", start +1);
                    bannedcountries.add(str.substring(start+1,end));
                    str=str.substring(end+1);

                    pos = str.indexOf(" ");
                }
            }


            if(Integer.parseInt(amount)<=0 || Integer.parseInt(price)<=0){
                connections.send(connectionId,"ERROR request addmovie failed");
                return;
            }

            //System.out.println("PRICE: "+price+" AMOUNT: "+amount);
            Movie toAdd = new Movie();
            maxMovieId = maxMovieId+1;
            toAdd.setId(maxMovieId.toString());
            toAdd.setName(moviename);
            toAdd.setPrice(Integer.parseInt(price));
            toAdd.setTotalAmount(Integer.parseInt(amount));
            toAdd.setAvailableAmount(Integer.parseInt(amount));
            toAdd.setBannedCountries(bannedcountries);

            synchronized (lock){
                moviesInfo.put(moviename,toAdd);
                movies.add(toAdd);
                updateMoviesJSON();
            }

            connections.send(connectionId,"ACK addmovie \""+moviename+"\" success");

            connections.broadcast("BROADCAST movie \""+moviename+"\" "
                                    +Integer.parseInt(amount)+" "+Integer.parseInt(price));


        }

    }

    protected void remMovie(String str) throws IOException {

        int pos3 = str.indexOf("\"");
        String moviename=str.substring(0,pos3);

        if(!moviesInfo.containsKey(moviename)){
            connections.send(connectionId,"ERROR request remmovie failed");
            return;
        }
        else{
            if(moviesInfo.get(moviename).getAvailableAmount()!=moviesInfo.get(moviename).getTotalAmount()){
                connections.send(connectionId,"ERROR request remmovie failed");
                return;
            }
            else{
                Movie toRemove = moviesInfo.get(moviename);

                synchronized (lock){
                    moviesInfo.remove(moviename);
                    movies.remove(toRemove);
                    updateMoviesJSON();
                }

                connections.send(connectionId,"ACK remmovie \""+moviename+"\" success");
                connections.broadcast("BROADCAST movie \""+moviename+"\" removed");

            }
        }

    }


    protected void changePrice(String str) throws IOException {
        int pos3 = str.indexOf("\"");
        String moviename=str.substring(0,pos3);

        if(!moviesInfo.containsKey(moviename)){
            connections.send(connectionId,"ERROR request changeprice failed");
            return;
        }
        else{
            int pos2 = str.lastIndexOf(" ");
            Integer price = Integer.parseInt(str.substring(pos2+1));

            if(price<=0){
                connections.send(connectionId,"ERROR request changeprice failed");
                return;
            }
            else{

                Integer copies = moviesInfo.get(moviename).getAvailableAmount();
                synchronized (lock){
                    moviesInfo.get(moviename).setPrice(price);
                    updateMoviesJSON();
                }
                connections.send(connectionId,"ACK changeprice \""+moviename+"\" success");
                connections.broadcast("BROADCAST movie \""+moviename+"\" "+copies+" "+price);//TODO: broadcast
            }

        }

    }



    @Override
    protected void request(String str) throws IOException {

        String username = this.loggedIn.get(connectionId);

        if(str.equals("balance info")){
            balanceInfo();
        }
        else if(str.contains("balance add ")){
            balanceAdd(str);
        }
        else if(str.equals("info")){//all movies
            info();


        }else if(str.contains("info \"")){//specific movie
            infoMovie(str);

        }
        else if (str.contains("rent ")) {
            rentMovie(str);
        }
        else if(str.contains("return ")){
            returnMovie(str);
        }
        else {//maybe admin
            int pos1 = str.indexOf(" ");
            String requestType = str.substring(0,pos1);
            str = str.substring(pos1+2);
            //check admin
            if(!usersInfo.get(username).getType().equals("admin")){
                connections.send(connectionId,"ERROR request "+requestType+" failed");
            }
            else {//yes admin
                int pos3 = str.indexOf("\"");
                String moviename = str.substring(0, pos3);

                //addmovie
                if (requestType.equals("addmovie")) {
                    addMovie(str);
                }
                //remove movie
                else if (requestType.equals("remmovie")) {
                    remMovie(str);
                }
                // change price
                else if (requestType.equals("changeprice")) {
                    changePrice(str);
                }
            }
        }
    }
}
