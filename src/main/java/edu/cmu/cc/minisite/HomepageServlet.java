package edu.cmu.cc.minisite;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * Task 3: Implement your logic to return all the comments authored by this
 * user.
 *
 * You should sort the comments by ups in descending order (from the largest to
 * the smallest one). If there is a tie in the ups, sort the comments in
 * descending order by their timestamp.
 */
public class HomepageServlet extends HttpServlet {

    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include the
     * credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables. export
     * MONGO_HOST=...
     */
    private static final String MONGO_HOST = System.getenv("MONGO_HOST");
    /**
     * MongoDB server URL.
     */
    private static final String URL = "mongodb://" + MONGO_HOST + ":27017";
    /**
     * Database name.
     */
    private static final String DB_NAME = "reddit_db";
    /**
     * Collection name.
     */
    private static final String COLLECTION_NAME = "posts";
    /**
     * MongoDB connection.
     */
    private static MongoCollection<Document> collection;

    /**
     * Initialize the connection.
     */
    public HomepageServlet() {
        Objects.requireNonNull(MONGO_HOST);
        MongoClientURI connectionString = new MongoClientURI(URL);
        MongoClient mongoClient = new MongoClient(connectionString);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
    }

    /**
     * Implement this method.
     *
     * @param request the request object that is passed to the servlet
     * @param response the response object that the servlet uses to return the
     * headers to the client
     * @throws IOException if an input or output error occurs
     * @throws ServletException if the request for the HEAD could not be handled
     */
    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        JsonObject result = new JsonObject();
        String id = request.getParameter("id");
        // try to get the comments
        try {
            JsonArray comments = fetchUserComments(id);
            result.add("comments", comments);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.addProperty("error", "Internal server error.");
        }
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(result.toString());
        writer.close();
    }

    /**
     * Get the comments by the user id.
     *
     * @param userId the user id
     * @return the comments by the user id
     */
    private JsonArray fetchUserComments(String userId) {
        // filter by user id 
        Document f = new Document("uid", userId);
        // sort by ups then timestamp
        Document s = new Document("ups", -1).append("timestamp", -1);
        // exclude the id field
        Document p = new Document("_id", 0);

        MongoCursor<Document> cursor = collection.find(f)
                .sort(s).projection(p).iterator();

        JsonArray comments = new JsonArray();
        // BSON to JSON
        try {
            while (cursor.hasNext()) {
                Document d = cursor.next();
                JsonObject json = new JsonParser().parse(d.toJson()).getAsJsonObject();
                comments.add(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new JsonArray();
        } finally {
            cursor.close();
        }
        return comments;
    }
}
