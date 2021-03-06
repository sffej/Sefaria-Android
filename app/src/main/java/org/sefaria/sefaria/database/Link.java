package org.sefaria.sefaria.database;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sefaria.sefaria.Settings;

public class Link {//implements Parcelable {

    public Link(Cursor cursor){
        lid = cursor.getInt(0);
        connType = cursor.getString(1);
        bid = cursor.getInt(2);
        final int LEVELS_START_NUM = 3;
        for(int i = 0;i<NUM_LEVELS;i++) {
            levels[i] = cursor.getInt(i + LEVELS_START_NUM);
        }
    }

    public int lid;
    public String connType;
    public int bid;
    private final static int NUM_LEVELS = 4;
    public int [] levels = new int [NUM_LEVELS];

    @Override
    public String toString(){
        String str =  String.valueOf(lid) + " " + connType + " bid: " + String.valueOf(bid);
        for(int i=0;i<NUM_LEVELS;i++){
            str += ","  + String.valueOf(levels[i]);
        }
        return str;
    }
     /*
    private static final String TABLE_LINKS = "Links" ;

    private static String addLevelWhere(String levelName, int levelNum, String linkTableName){
        return addLevelWhere(levelName, levelNum, linkTableName, -1);
    }
    private static String addLevelWhere(String levelName, int levelNum, String linkTableName, int wherePage){
        if(levelNum == 0)//it means this level doesn't exist (it's too high), so don't display anything.
            return " ";
        else if(levelNum == -1){//it means that we want to display everything from this level
            levelNum =  0;
        }
        return " AND (" + linkTableName + "." + levelName  + "=" + String.valueOf(levelNum)// + " OR " + linkTableName +"." + levelName + "=0"
                + ") ";
    }

    private static String makeWhereStatement(Segment segment){
        String str = " L." + Kbida + "=" + segment.bid;
        for(int i=0;i<segment.levels.length;i++)
            str += addLevelWhere(Klevel1a, segment.levels[0],"L");
        str += " AND    L.bidb=T.bid "
                + " AND (T." + Segment.Klevel1 + "=L." + Klevel1b + ")"// + " OR L." + Klevel1b + "=0)"
                + " AND (T." + Segment.Klevel2 + "=L." + Klevel2b + ")"//+ " OR L." + Klevel2b + "=0)"
                + " AND (T." + Segment.Klevel3 + "=L." + Klevel3b + ")"//+ " OR L." + Klevel3b + "=0)"
                + " AND (T." + Segment.Klevel4 + "=L." + Klevel4b + ")"//+ " OR L." + Klevel4b + "=0)"
                //+ " AND (T." + Segment.Klevel5 + "=L." + Klevel5b + ")"//+ " OR L." + Klevel5b + "=0)"
                //+ " AND (T." + Segment.Klevel6 + "=L." + Klevel6b + ")"//+ " OR L." + Klevel6b + "=0)"
        ;
        return str;
    }

    private static String makeWhereStatement2(Segment segment){
        String str = " L2." + Kbidb + "=" + segment.bid ;
        for(int i=0;i<segment.levels.length;i++)
            str += addLevelWhere(Klevel1b, segment.levels[i], "L2");
        str += " AND    L2.bida=T3.bid "
                + " AND (T3." + Segment.Klevel1 + "=L2." + Klevel1a + ")"//+ " OR L2." + Klevel1a + "=0)"
                + " AND (T3." + Segment.Klevel2 + "=L2." + Klevel2a + ")"//+ " OR L2." + Klevel2a + "=0)"
                + " AND (T3." + Segment.Klevel3 + "=L2." + Klevel3a + ")"//+ " OR L2." + Klevel3a + "=0)"
                + " AND (T3." + Segment.Klevel4 + "=L2." + Klevel4a + ")"//+ " OR L2." + Klevel4a + "=0)"
                //+ " AND (T3." + Segment.Klevel5 + "=L2." + Klevel5a + ")"//+ " OR L2." + Klevel5a + "=0)"
                //+ " AND (T3." + Segment.Klevel6 + "=L2." + Klevel6a + ")" ; //+ " OR L2." + Klevel6a + "=0)";
        return str;
    }
    */



    /**
     * Get links for specific segment (ex. verse).
     * @param segment
     * @param limit
     * @param offset
     * @return linkList
     */

    /**
     *
     * @param segment
     * @param linkFilter null if no filter or linkCount containing anything you want included in the filter (including LinkFilter linkfiler's children)
     * @return List<Segment> for texts links to the input segment
     */
    public static List<Segment> getLinkedTexts(Segment segment, LinkFilter linkFilter) throws API.APIException, Book.BookNotFoundException  {
        List<Segment> linkList;
        if(segment.tid == 0 || Settings.getUseAPI()){ //tid might be 0 if it was gotten using API (So for example with alternate segment versions)
            linkList = getLinkedTextsFromAPI(segment,linkFilter);
        }else{
            linkList = getLinkedTextsFromDB(segment, linkFilter);
        }
        return linkList;
    }


    public static List<Segment> getLinkedTextsFromAPI(Segment orgSegment, LinkFilter linkFilter) throws API.APIException, Book.BookNotFoundException {
        Log.d("API.Link","got starting LinksAPI");
        List<Segment> segments = new ArrayList<>();
        String place = orgSegment.getURL(false, false);
        String url = API.LINK_URL + place;
        String data = API.getDataFromURL(url);
        Log.d("API.Link","got data");
        Book book;
        try {
            book = new Book(orgSegment.bid);
        } catch (Book.BookNotFoundException e) {
            return segments;
        }
        if(data.length()==0)
            return segments;
        List<Segment> commentaries = new ArrayList<>();

        String commentOn = " on " + book.title;
        try {
            JSONArray linksArray = new JSONArray(data);
            //Log.d("api", "jsonData:" + jsonData.toString());
            for(int i=0;i<linksArray.length();i++){
                try {
                    JSONObject jsonLink = linksArray.getJSONObject(i);
                    String enTitle = jsonLink.getString("index_title");
                    String category = jsonLink.getString("category");
                    String ref = jsonLink.getString("ref");
                    if (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.ALL ||
                            (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT && category.equals(linkFilter.enTitle)) ||
                            (linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK && enTitle.equals(linkFilter.enTitle))
                            ) {
                        Segment tempSegment = new Segment(removeEmpty(jsonLink.getString("text")), removeEmpty(jsonLink.getString("he")), Book.getBid(enTitle), ref);
                        if (category.equals("Commentary"))
                            commentaries.add(tempSegment);
                        else
                            segments.add(tempSegment);
                    }
                }catch (Exception e1){
                    e1.printStackTrace();
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        Collections.sort(commentaries,compareTexts);
        Collections.sort(segments, compareTexts);
        segments.addAll(0,commentaries);

        Log.d("API.Link","finished LinksAPI");
        return segments;
    }

    static Comparator<Segment> compareTexts = new Comparator<Segment>() {
        @Override
        public int compare(Segment a, Segment b) {
            //only sorting on bid. Within same book using stable sort to keep order
            return a.bid - b.bid;
        }
    };

    private static String removeEmpty(String str){
        if(str.equals("[]"))
            return "";
        else
            return str;
    }

    private static List<Segment> getLinkedTextsFromDB(Segment segment, LinkFilter linkFilter) {
        SQLiteDatabase db = Database.getDB();
        List<Segment> linkList = new ArrayList<>();

        //Log.d("getLinksTextsFromDB", "Started ... linkFiler:" + linkFilter);

        String sql = "SELECT T.* FROM " + Segment.TABLE_TEXTS + " T, Books B WHERE T.bid = B._id AND T._id"
                + " IN ( SELECT L1.tid2 FROM Links_small L1 WHERE L1.tid1 = " + segment.tid
                + " UNION "
                + " SELECT L2.tid1 FROM Links_small L2 WHERE L2.tid2 = " + segment.tid
                + ")";

        String [] args = null;
        if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.CAT){
            if(linkFilter.enTitle.equals(LinkFilter.COMMENTARY)){
                sql += " AND B.commentsOn = ? ";
                args = new String[] {""+ segment.bid};
            }else{
                String category;
                if(linkFilter.enTitle.equals(LinkFilter.QUOTING_COMMENTARY)) {
                    //the category in the database is simply "Commentary"
                    category = "Commentary";
                    //don't include the commentary that is directly for this book (like "Rashi on Genesis" for "Genesis")
                    sql += " AND B.commentsOn <> " + segment.bid;
                }else {
                    category = linkFilter.enTitle;
                }

                //get the string for categories start with the selected category
                //if(Build.VERSION.SDK_INT >= 21) sql += " AND B.categories like printf('%s%s%s','[\"',?,'%') "; else
                sql += " AND B.categories like '[\"' || ? || '%' ";

                args = new String[]{category};

            }
        }else if(linkFilter.depth_type == LinkFilter.DEPTH_TYPE.BOOK){
            sql += " AND B.title = ?";
            args = new String[]{linkFilter.enTitle};
        }

        sql += " ORDER BY (case when B.commentsOn=" + segment.bid  + " then 0 else 1 end), T.bid";



        Cursor cursor = db.rawQuery(sql, args);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Segment(cursor));
            } while (cursor.moveToNext());
        }

        //Log.d("getLinksTextsFromDB", "Finished ... linkList.size():" + linkList.size());
        if(linkList.size()!= linkFilter.count && linkList.size() < 7){
            for(LinkFilter lc: linkFilter.getChildren()){
                Log.d("Link", lc.toString());
            }
            for(Segment link:linkList){
                link.log();
            }
        }
        return linkList;
    }



    /**
     * gets links to a particular level other than the last level
     * @param segment
     * @param limit
     * @param offset
     * @return
     */
    /*
    public static List<Segment> getLinkedChapTexts(Segment segment, int limit, int offset) {
        List<Segment> texts = new ArrayList<Segment>();
        Segment dummyChapText = Segment.makeDummyChapText(segment);
        try{
            texts = getLinkedChapTextsFromDB(dummyChapText, limit, offset);
        }catch(SQLiteException e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }
            texts = API.getChapLinks(dummyChapText,limit,offset);
        }catch(Exception e){
            e.printStackTrace();
        }

        return texts;

    }


    private static List<Segment> getLinkedChapTextsFromDB(Segment segment, int limit, int offset) {
        Database dbHandler = Database.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Segment> linkList = new ArrayList<Segment>();
        String whereStatement = makeWhereStatement(segment);
        String whereStatement2 = makeWhereStatement2(segment);

        String select = "SELECT T2.* FROM " + Segment.TABLE_TEXTS + " T2 WHERE T2._id"
                + " IN (  SELECT T._id"
                + " FROM " + TABLE_LINKS +" L, " + Segment.TABLE_TEXTS + " T "
                + " WHERE " + whereStatement
                + ")";

        String select2 = "SELECT T4.* FROM " + Segment.TABLE_TEXTS + " T4 WHERE T4._id"
                + " IN (  SELECT T3._id"
                + " FROM " + TABLE_LINKS +" L2, " + Segment.TABLE_TEXTS + " T3 "
                + " WHERE " + whereStatement2
                + ")";

        String sql = select + " UNION " + select2 //"( " + select + " )";// + " UNION " + "( " + select2 + " )"
                + "ORDER BY bid"
                + " LIMIT " + String.valueOf(limit)
                + " OFFSET " + String.valueOf(offset)
                ;
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Segment(cursor));
            } while (cursor.moveToNext());
        }
        return linkList;
    }


    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Link> CREATOR
            = new Parcelable.Creator<Link>() {
        public Link createFromParcel(Parcel in) {
            return new Link(in);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(lid);
        dest.writeString(connType);
        dest.writeInt(bid);
        for(int i =0;i<NUM_LEVELS;i++)
            dest.writeInt(levels[i]);


    }

    private Link(Parcel in) {
        lid = in.readInt();
        connType = in.readString();
        bid = in.readInt();
        for(int i =0;i<NUM_LEVELS;i++)
            levels[i] = in.readInt();
    }
    */
}


