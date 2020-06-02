package com.ruijie.listenevent.utils;

import java.net.URI;

/**
 * <p>Title:      </p>
 * <p>Description:
 * <p>Copyright: Copyright (c) 2019     </p>
 * <p>Company: Ruijie Co., Ltd.          </p>
 * <p>Create Time: 2020/4/28 11:50       </p>
 *
 * @author lijianhua
 * <p>Update Time:                      </p>
 * <p>Updater:                          </p>
 * <p>Update Comments:                  </p>
 */
public final class UrlUtils {

    /**
     * The default size to use for string buffers.
     */
    private static final int DEFAULT_BUFFER_SIZE = 64;

    /**
     * Utility Class.
     */
    private UrlUtils() {
        //do nothing
    }

    /**
     * Join two paths together taking into account leading/trailing slashes.
     * @param path1 the first path
     * @param path2 the second path
     * @return the joins path
     */
    public static String join(final String path1, final String path2) {
        if (path1.isEmpty() && path2.isEmpty()) return "";
        if (path1.isEmpty() && !path2.isEmpty()) return path2;
        if (path2.isEmpty() && !path1.isEmpty()) return path1;
        final StringBuilder sb = new StringBuilder(DEFAULT_BUFFER_SIZE);
        sb.append(path1);
        if (sb.charAt(sb.length() - 1) == '/') sb.setLength(sb.length() - 1);
        if (path2.charAt(0) != '/') sb.append('/');
        sb.append(path2);
        return sb.toString();
    }



    /**
     * Create a JSON URI from the supplied parameters.
     * @param uri the server URI
     * @param context the server context if any
     * @param path the specific API path
     * @return new full URI instance
     */
    public static URI toJsonApiUri(final URI uri, final String context, final String path) {
        String p = path;
        if (!p.matches("(?i)https?://.*")) p = join(context, p);

        if (!p.contains("?")) {
            p = join(p, "api/json");
        } else {
            final String[] components = p.split("\\?", 2);
            p = join(components[0], "api/json") + "?" + components[1];
        }
        return uri.resolve("/").resolve(p.replace(" ", "%20"));
    }



    /**
     * Create a URI from the supplied parameters.
     * @param uri the server URI
     * @param context the server context if any
     * @param path the specific API path
     * @return new full URI instance
     */
    public static URI toNoApiUri(final URI uri, final String context, final String path) {
        final String p = path.matches("(?i)https?://.*") ? path : join(context, path);
        return uri.resolve("/").resolve(p);
    }



}
