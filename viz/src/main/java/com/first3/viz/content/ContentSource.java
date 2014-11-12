/*
 * Copyright 2012-2014, First Three LLC
 *
 * This file is a part of Viz.
 *
 * Viz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Viz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Viz.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.first3.viz.content;

import com.first3.viz.builders.BlinkxResourceBuilder;
import com.first3.viz.builders.DailyMotionResourceBuilder;
import com.first3.viz.builders.FunnyOrDieResourceBuilder;
import com.first3.viz.builders.GenericResourceBuilder;
import com.first3.viz.builders.GoGoAnimeResourceBuilder;
import com.first3.viz.builders.LiveleakResourceBuilder;
import com.first3.viz.builders.MetacafeResourceBuilder;
import com.first3.viz.builders.NovamovResourceBuilder;
import com.first3.viz.builders.Play44ResourceBuilder;
import com.first3.viz.builders.PornHubBuilder;
import com.first3.viz.builders.RedtubeBuilder;
import com.first3.viz.builders.ResourceBuilder;
import com.first3.viz.builders.VevoResourceBuilder;
import com.first3.viz.builders.Video44ResourceBuilder;
import com.first3.viz.builders.VideoFunResourceBuilder;
import com.first3.viz.builders.VidzurResourceBuilder;
import com.first3.viz.builders.VimeoResourceBuilder;
import com.first3.viz.builders.YouruploadResourceBuilder;
import com.first3.viz.utils.Utils;

public class ContentSource {
    private final String fSite;
    private final String[] fHostnames;
    private final ResourceBuilder fResourceBuilder;
    private final String fURL;
    private final int fRank;
    private final boolean addToFavorites;

    /**
     * @param rank position in bookmarks (if added)
     * @param site  Display name to user, can be anything.
     * @param hostname the string (minus http://) that will be used to match
     *                 content against this source
     * @param url the url to redirect the user to the site when selected on
     *            favorites
     * @param addToFavorites whether the ContentSource should be displayed as
     *      a default bookmark.  If true, the favorite will be added on app
     *      upgrade.
     */
    private ContentSource(int rank, String site, String[] hostnames, String url,
                          ResourceBuilder builder, boolean addToFavorites) {
        this.fRank = rank;
        this.fSite = site;
        this.fHostnames = hostnames;
        this.fResourceBuilder = builder;
        this.fURL = url;
        this.addToFavorites = addToFavorites;
    }

    public boolean addToFavorites() {
        return addToFavorites;
    }

    /**
     * A name for this ContentSource that is suitable for user display.
     */
    @Override
    public String toString() { return getSite(); }

    public String getSite() { return this.fSite; }

    /**
     * Relative position of this ContentSource when displayed as a Favorite.
     */
    public int getRank() { return this.fRank; }

    /**
     * These are the strings that are used to see if a url matches this particular
     * ContentSource.  It is compared directly against the url.getHost().
     */
    public String[] getHostnames()     { return this.fHostnames;  }

    /**
     * The ResourceBuilder that can construct a Resource from this
     * ContentSource.
     */
    public ResourceBuilder getResourceBuilder() {
        return this.fResourceBuilder;
    }

    /**
     * The URL that should be loaded if a user wanted to go directly to
     * this ContentSource.  In many cases this will be the same as the
     * hostname with only the necessary http:// added.
     */
    public String getURL() {
        return fURL;
    }

    /**
     * The GENERIC content source is not added as a content source; it is used if a
     * match isn't found among the remaining ContentSources.
     */
    public static final ContentSource GENERIC =
        new ContentSource(0, "Generic", new String[] { "anyhost" }, "anyurl",
                new GenericResourceBuilder(), false);

    public static final ContentSource VIMEO =
        new ContentSource(1, "Vimeo", new String[] { "vimeo.com" }, "http://vimeo.com/m",
                new VimeoResourceBuilder(), true);

    public static final ContentSource DM =
            new ContentSource(2, "Dailymotion", new String[] { "touch.dailymotion.com",
                "www.dailymotion.com" }, "http://touch.dailymotion.com",
                new DailyMotionResourceBuilder(), true);

    public static final ContentSource VEVO =
            new ContentSource(3, "Vevo", new String[] { "www.vevo.com" },
                    "http://www.vevo.com", new VevoResourceBuilder(), Utils.isKitkatOrHigher());

    public static final ContentSource FOD =
            new ContentSource(4, "Funny or Die", new String[] { "www.funnyordie.com" },
                    "http://www.funnyordie.com", new FunnyOrDieResourceBuilder(), true);

    public static final ContentSource MCAFE =
            new ContentSource(5, "Metacafe", new String[] { "www.metacafe.com" },
                    "http://www.metacafe.com", new MetacafeResourceBuilder(), true);

    public static final ContentSource BLINKX =
            new ContentSource(6, "Blinkx", new String[] { "www.blinkx.com","blinkx.com","cdn.blinkx.com" },
                    "http://www.blinkx.com", new BlinkxResourceBuilder(), true);

    public static final ContentSource LIVELEAK =
            new ContentSource(7, "LiveLeak", new String[] { "www.liveleak.com" },
                    "http://www.liveleak.com", new LiveleakResourceBuilder(), true);

    public static final ContentSource GOGOANIME = new ContentSource(3, "GoGoAnime",
                    new String[] { "www.gogoanime.com" }, "http://www.gogoanime.com/",
                    new GoGoAnimeResourceBuilder(), true);

    public static final ContentSource REDTUBE = new ContentSource(8, "Redtube", new String[] { "redtube.com",
                    "www.redtube.com" }, "http://www.redtube.com", new RedtubeBuilder(), false);

    public static final ContentSource PORNHUB = new ContentSource(0, "Pornhub", new String[] { "m.pornhub.com" },
                    "http://m.pornhub.com", new PornHubBuilder(), false);

    public static final ContentSource VIDEOFUN = new ContentSource(0, "", new String[] { "videofun.me" },
                    "http://www.videofun.me/", new VideoFunResourceBuilder(), false);

    public static final ContentSource VIDEO44 = new ContentSource(0, "", new String[] { "www.video44.net" },
                    "http://www.video44.net/", new Video44ResourceBuilder(), false);

    public static final ContentSource VIDZUR = new ContentSource(0, "", new String[] { "vidzur.com" },
                    "http://www.vidzur.com/", new VidzurResourceBuilder(), false);

    public static final ContentSource YOURUPLOAD = new ContentSource(0, "", new String[] { "embed.yourupload.com" },
                    "http://yourupload.com", new YouruploadResourceBuilder(), false);

    public static final ContentSource NOVAMOV = new ContentSource(0, "", new String[] { "embed.novamov.com" },
                    "http://www.novamov.com/", new NovamovResourceBuilder(), false);

    public static final ContentSource PLAY44 = new ContentSource(0, "", new String[] { "play44.net" },
                    "http://www.play44.com/", new Play44ResourceBuilder(), false);
}
