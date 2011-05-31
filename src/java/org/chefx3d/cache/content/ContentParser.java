/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.chefx3d.cache.content;

import java.io.InputStream;

/**
 *
 * A interface to define parsers that accepts a inputstream and
 * returns an object.
 *
 * @param <O> Object, or subclass of Object
 * @param <I> Inputstream or subclass of InputStream
 * @author djoyce
 */
public interface ContentParser<O extends Object,I extends InputStream> {

    /** Parse an instance of I, and return an instance of O
     * @param is or subclass
     * @return Object or subclass of Object
     */
    public O parse(I is);

}
