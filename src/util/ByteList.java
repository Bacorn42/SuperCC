package util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class is basically an ArrayList for the byte primitive, used for
 * storing moves. The reason I don't just use ArrayList[Byte] is to make
 * copying and String conversions simpler and faster.
 *
 * The initial capacity is set to 200. This doubles when it is reached. The
 * capacity never decreases.
 */
public class ByteList implements Iterable<Byte> {
    
    private static final int INITIAL_CAPACITY = 200;
    
    private byte[] bytes = new byte[INITIAL_CAPACITY];
    private int size = 0;
    private int capacity = INITIAL_CAPACITY;
    
    /**
     * Appends the specified element to the end of this list.
     * @param b byte to be appended to this list
     */
    public void add(byte b){
        bytes[size++] = b;
        if (size == capacity){
            capacity *= 2;
            bytes = Arrays.copyOf(bytes, capacity);
        }
    }
    
    /**
     * Removes the element at the end of this list.
     */
    public void removeLast(){
        size--;
    }
    
    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element).
     *
     * The returned array will be "safe" in that no references to it are
     * maintained by this list. (In other words, this method must allocate a
     * new array). The caller is thus free to modify the returned array.
     *
     * This method acts as bridge between array-based and collection-based APIs.
     *
     * @return an array containing all of the elements in this list in proper
     * sequence
     */
    public byte[] toArray(){
        return Arrays.copyOf(bytes, size);
    }
    
    /**
     * Returns the number of elements in this list.
     * @return the number of elements in this list
     */
    public int size(){
        return size;
    }
    
    /**
     * The default constructor for ByteList. The initial capacity is set to
     * INITIAL_CAPACITY.
     */
    public ByteList(){};
    
    // Constructor used for cloning
    private ByteList(byte[] moves, int size, int capacity){
        this.bytes = moves;
        this.size = size;
        this.capacity = capacity;
    }
    
    /**
     * Returns an iterator over the elements in this list in proper sequence.
     * @return an iterator over the elements in this list in proper sequence
     */
    @Override
    public Iterator<Byte> iterator() {
        Iterator<Byte> it = new Iterator<>() {
            
            private int i = 0;
            
            @Override
            public boolean hasNext() {
                return i < size;
            }
            
            @Override
            public Byte next() {
                return bytes[i++];
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
        return it;
    }
    
    @Override
    public ByteList clone(){
        return new ByteList(Arrays.copyOf(bytes, capacity), size, capacity);
    }
    
    /**
     * Converts the bytes into a String.
     * Use of this method is not recommended - use toString(Charset charset)
     * instead.
     * @return The bytes as a string
     */
    @Override
    public String toString(){
        return new String(bytes, 0, size);
    }
    
    /**
     * Converts the bytes into a String, using a user-defined charset encoding.
     * @param charset The charset to encode to
     * @return The bytes as a string
     */
    public String toString(Charset charset){
        return new String(bytes, 0, size, charset);
    }
    
}
