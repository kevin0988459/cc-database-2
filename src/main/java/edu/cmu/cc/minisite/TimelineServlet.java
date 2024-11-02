package edu.cmu.cc.minisite;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * In this task you will populate a user's timeline. This task helps you
 * understand the concept of fan-out. Practice writing complex fan-out queries
 * that span multiple databases.
 *
 * Task 4 (1): Get the name and profile of the user as you did in Task 1 Put
 * them as fields in the result JSON object
 *
 * Task 4 (2); Get the follower name and profiles as you did in Task 2 Put them
 * in the result JSON object as one array
 *
 * Task 4 (3): From the user's followees, get the 30 most popular comments and
 * put them in the result JSON object as one JSON array. (Remember to find their
 * parent and grandparent)
 *
 * The posts should be sorted: First by ups in descending order. Break tie by
 * the timestamp in descending order.
 */
public class TimelineServlet extends HttpServlet {

    /**
     * Your initialization code goes here.
     */
    private ProfileServlet profileServlet;
    private FollowerServlet followerServlet;
    private HomepageServlet homepageServlet;

    /**
     * Initializes servlet instances.
     *
     * @throws ServletException if an initialization error occurs.
     */
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            profileServlet = new ProfileServlet();
            followerServlet = new FollowerServlet();
            homepageServlet = new HomepageServlet();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new ServletException("Initialization failed: " + e.getMessage());
        }
    }

    /**
     * Cleans up resources by closing connections and servlet instances.
     */
    @Override
    public void destroy() {
        super.destroy();

        // Close ProfileServlet resources
        if (profileServlet != null) {
            profileServlet.closeConnection();
        }

        // Close FollowerServlet resources
        if (followerServlet != null) {
            followerServlet.closeDriver();
        }

        // Close HomepageServlet resources
        if (homepageServlet != null) {
            homepageServlet.closeCollection();
        }
    }

    /**
     * Don't modify this method.
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

        // DON'T modify this method.
        String id = request.getParameter("id");
        String result = getTimeline(id);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.print(result);
        writer.close();
    }

    /**
     * Method to get given user's timeline.
     *
     * @param id user id
     * @return timeline of this user
     */
    private String getTimeline(String id) {
        JsonObject result = new JsonObject();
        try {
            // result add followers
            JsonArray followers = followerServlet.getFollowers(id);
            result.add("followers", followers);

            // get followees to get comments and add to result
            JsonArray followeesArray = followerServlet.getFollowees(id);
            List<String> followeeIds = new ArrayList<>();
            for (int i = 0; i < followeesArray.size(); i++) {
                JsonObject followee = followeesArray.get(i).getAsJsonObject();
                followeeIds.add(followee.get("name").getAsString());
            }
            JsonArray comments = homepageServlet.getTopCommentsFromFollowees(followeeIds, 30);
            result.add("comments", comments);

            // get profile, name and add to result
            String profile = profileServlet.getProfile(id);
            result.addProperty("profile", profile);
            result.addProperty("name", id);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
