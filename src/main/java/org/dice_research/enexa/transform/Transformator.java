package org.dice_research.enexa.transform;

import java.io.File;
import java.io.IOException;

import javax.annotation.CheckForNull;

public interface Transformator extends AutoCloseable {

    /**
     * Add the given files to the target file. If the array contains directories,
     * all files and sub directories are added. Note that <b>all</b> files must be
     * RDF files.
     * 
     * <p>
     * Note: it is suggested to use {@link #addFile2Stream(File, String)} with an
     * explicit content type for each file.
     * </p>
     * 
     * @param files an array of RDF files that should be added to the output file
     * @throws IOException if an IO error occurs either during reading the given
     *                     files or writing the read data to the output file.
     */
    default void addFiles2Stream(File[] files) throws IOException {
        for (File file : files) {
            addFile2Stream(file);
        }
    }

    /**
     * Add the given file to the target file. If the given file is a directory, all
     * files and sub directories are added. Note that <b>all</b> files must be RDF
     * files.
     * 
     * <p>
     * Note: it is suggested to use {@link #addFile2Stream(File, String)} with an
     * explicit content type for each file.
     * </p>
     * 
     * @param file an RDF file or a directory with RDF files that should be added to
     *             the output file
     * @throws IOException if an IO error occurs either during reading the given
     *                     file(s) or writing the read data to the output file.
     */
    default void addFile2Stream(File file) throws IOException {
        if (file.isDirectory()) {
            addFiles2Stream(file.listFiles());
        } else {
            addFile2Stream(file, null);
        }
    }

    /**
     * Add the given file to the target file. The given content type is used to
     * guide the parsing of the file. If the given file is a directory, all files
     * and sub directories are added. Note that <b>all</b> files must be RDF files.
     * 
     * @param file        an RDF file or a directory with RDF files that should be
     *                    added to the output file
     * @param contentType the content type String expressing the RDF serialization
     *                    of the given file. The String is ignored if it is
     *                    {@code null}, which means that the identification of the
     *                    serialization is solely based on the file name.
     * @throws IOException if an IO error occurs either during reading the given
     *                     file(s) or writing the read data to the output file.
     */
    void addFile2Stream(File file, @CheckForNull String contentType) throws IOException;

    /**
     * @return the outputFile
     */
    File getOutputFile();

    /**
     * Creates a new {@link TransformatorBuilder} instance to build a new
     * {@link Transformator}.
     * 
     * @return a new {@link TransformatorBuilder} instance to build a new
     *         {@link Transformator}.
     */
    public static TransformatorBuilder builder() {
        return new TransformatorBuilder();
    }
}
