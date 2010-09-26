/*
 * Copyright (c) 2002-2010 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.opencl;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.PointerBuffer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.*;

/**
 * Utility class for OpenCL API calls.
 * TODO: Remove useless stuff
 *
 * @author spasi
 */
final class APIUtil {

	private static final int INITIAL_BUFFER_SIZE = 256;
	private static final int INITIAL_LENGTHS_SIZE = 4;

	private static final int BUFFERS_SIZE = 32;

	private static final ThreadLocal<char[]> arrayTL = new ThreadLocal<char[]>() {
		protected char[] initialValue() { return new char[INITIAL_BUFFER_SIZE]; }
	};

	private static final ThreadLocal<ByteBuffer> bufferByteTL = new ThreadLocal<ByteBuffer>() {
		protected ByteBuffer initialValue() { return BufferUtils.createByteBuffer(INITIAL_BUFFER_SIZE); }
	};

	private static final ThreadLocal<PointerBuffer> bufferPointerTL = new ThreadLocal<PointerBuffer>() {
		protected PointerBuffer initialValue() { return BufferUtils.createPointerBuffer(INITIAL_BUFFER_SIZE); }
	};

	private static final ThreadLocal<PointerBuffer> lengthsTL = new ThreadLocal<PointerBuffer>() {
		protected PointerBuffer initialValue() { return BufferUtils.createPointerBuffer(INITIAL_LENGTHS_SIZE); }
	};

	private static final ThreadLocal<InfiniteCharSequence> infiniteSeqTL = new ThreadLocal<InfiniteCharSequence>() {
		protected InfiniteCharSequence initialValue() { return new InfiniteCharSequence(); }
	};

	private static final ThreadLocal<Buffers> buffersTL = new ThreadLocal<Buffers>() {
		protected Buffers initialValue() { return new Buffers(); }
	};

	private static final CharsetEncoder encoder = Charset.forName("US-ASCII").newEncoder();

	private static final ObjectDestructor<CLDevice> DESTRUCTOR_CLSubDevice = new ObjectDestructor<CLDevice>() {
		public void release(final CLDevice object) { EXTDeviceFission.clReleaseDeviceEXT(object); }
	};
	private static final ObjectDestructor<CLMem> DESTRUCTOR_CLMem = new ObjectDestructor<CLMem>() {
		public void release(final CLMem object) { CL10.clReleaseMemObject(object); }
	};
	private static final ObjectDestructor<CLCommandQueue> DESTRUCTOR_CLCommandQueue = new ObjectDestructor<CLCommandQueue>() {
		public void release(final CLCommandQueue object) { CL10.clReleaseCommandQueue(object); }
	};
	private static final ObjectDestructor<CLSampler> DESTRUCTOR_CLSampler = new ObjectDestructor<CLSampler>() {
		public void release(final CLSampler object) { CL10.clReleaseSampler(object); }
	};
	private static final ObjectDestructor<CLProgram> DESTRUCTOR_CLProgram = new ObjectDestructor<CLProgram>() {
		public void release(final CLProgram object) { CL10.clReleaseProgram(object); }
	};
	private static final ObjectDestructor<CLKernel> DESTRUCTOR_CLKernel = new ObjectDestructor<CLKernel>() {
		public void release(final CLKernel object) { CL10.clReleaseKernel(object); }
	};
	private static final ObjectDestructor<CLEvent> DESTRUCTOR_CLEvent = new ObjectDestructor<CLEvent>() {
		public void release(final CLEvent object) { CL10.clReleaseEvent(object); }
	};

	private APIUtil() {
	}

	private static char[] getArray(final int size) {
		char[] array = arrayTL.get();

		if ( array.length < size ) {
			int sizeNew = array.length << 1;
			while ( sizeNew < size )
				sizeNew <<= 1;

			array = new char[size];
			arrayTL.set(array);
		}

		return array;
	}

	static ByteBuffer getBufferByte(final int size) {
		ByteBuffer buffer = bufferByteTL.get();

		if ( buffer.capacity() < size ) {
			int sizeNew = buffer.capacity() << 1;
			while ( sizeNew < size )
				sizeNew <<= 1;

			buffer = BufferUtils.createByteBuffer(size);
			bufferByteTL.set(buffer);
		} else
			buffer.clear();

		return buffer;
	}

	private static ByteBuffer getBufferByteOffset(final int size) {
		ByteBuffer buffer = bufferByteTL.get();

		if ( buffer.capacity() < size ) {
			int sizeNew = buffer.capacity() << 1;
			while ( sizeNew < size )
				sizeNew <<= 1;

			final ByteBuffer bufferNew = BufferUtils.createByteBuffer(size);
			bufferNew.put(buffer);
			bufferByteTL.set(buffer = bufferNew);
		} else {
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
		}

		return buffer;
	}

	static PointerBuffer getBufferPointer(final int size) {
		PointerBuffer buffer = bufferPointerTL.get();

		if ( buffer.capacity() < size ) {
			int sizeNew = buffer.capacity() << 1;
			while ( sizeNew < size )
				sizeNew <<= 1;

			buffer = BufferUtils.createPointerBuffer(size);
			bufferPointerTL.set(buffer);
		} else
			buffer.clear();

		return buffer;
	}

	static ShortBuffer getBufferShort() { return buffersTL.get().shorts; }

	static IntBuffer getBufferInt() { return buffersTL.get().ints; }

	static IntBuffer getBufferIntDebug() { return buffersTL.get().intsDebug; }

	static LongBuffer getBufferLong() { return buffersTL.get().longs; }

	static FloatBuffer getBufferFloat() { return buffersTL.get().floats; }

	static DoubleBuffer getBufferDouble() { return buffersTL.get().doubles; }

	static PointerBuffer getBufferPointer() { return buffersTL.get().pointers; }

	static PointerBuffer getLengths() {
		return getLengths(1);
	}

	static PointerBuffer getLengths(final int size) {
		PointerBuffer lengths = lengthsTL.get();

		if ( lengths.capacity() < size ) {
			int sizeNew = lengths.capacity();
			while ( sizeNew < size )
				sizeNew <<= 1;

			lengths = BufferUtils.createPointerBuffer(size);
			lengthsTL.set(lengths);
		} else
			lengths.clear();

		return lengths;
	}

	private static InfiniteCharSequence getInfiniteSeq() {
		return infiniteSeqTL.get();
	}

	private static void encode(final ByteBuffer buffer, final CharSequence string) {
		final InfiniteCharSequence infiniteSeq = getInfiniteSeq();
		infiniteSeq.setString(string);
		encoder.encode(infiniteSeq.buffer, buffer, true);
		infiniteSeq.clear();
	}

	/**
	 * Reads a byte string from the specified buffer.
	 *
	 * @param buffer
	 *
	 * @return the buffer as a String.
	 */
	static String getString(final ByteBuffer buffer) {
		final int length = buffer.remaining();
		final char[] charArray = getArray(length);

		for ( int i = buffer.position(); i < buffer.limit(); i++ )
			charArray[i - buffer.position()] = (char)buffer.get(i);

		return new String(charArray, 0, length);
	}

	/**
	 * Returns a buffer containing the specified string as bytes.
	 *
	 * @param string
	 *
	 * @return the String as a ByteBuffer
	 */
	static ByteBuffer getBuffer(final CharSequence string) {
		final ByteBuffer buffer = getBufferByte(string.length());

		encode(buffer, string);

		buffer.flip();
		return buffer;
	}

	/**
	 * Returns a buffer containing the specified string as bytes, starting at the specified offset.
	 *
	 * @param string
	 *
	 * @return the String as a ByteBuffer
	 */
	static ByteBuffer getBuffer(final CharSequence string, final int offset) {
		final ByteBuffer buffer = getBufferByteOffset(offset + string.length());

		encode(buffer, string);

		buffer.flip();
		return buffer;
	}

	/**
	 * Returns a buffer containing the specified string as bytes, including null-termination.
	 *
	 * @param string
	 *
	 * @return the String as a ByteBuffer
	 */
	static ByteBuffer getBufferNT(final CharSequence string) {
		final ByteBuffer buffer = getBufferByte(string.length() + 1);

		encode(buffer, string);

		buffer.put((byte)0);
		buffer.flip();
		return buffer;
	}

	static int getTotalLength(final CharSequence[] strings) {
		int length = 0;
		for ( int i = 0; i < strings.length; i++ )
			length += strings[i].length();

		return length;
	}

	/**
	 * Returns a buffer containing the specified strings as bytes.
	 *
	 * @param strings
	 *
	 * @return the Strings as a ByteBuffer
	 */
	static ByteBuffer getBuffer(final CharSequence[] strings) {
		final ByteBuffer buffer = getBufferByte(getTotalLength(strings));

		final InfiniteCharSequence infiniteSeq = getInfiniteSeq();
		for ( int i = 0; i < strings.length; i++ ) {
			infiniteSeq.setString(strings[i]);
			encoder.encode(infiniteSeq.buffer, buffer, true);
		}
		infiniteSeq.clear();

		buffer.flip();
		return buffer;
	}

	/**
	 * Returns a buffer containing the specified strings as bytes, including null-termination.
	 *
	 * @param strings
	 *
	 * @return the Strings as a ByteBuffer
	 */
	static ByteBuffer getBufferNT(final CharSequence[] strings) {
		final ByteBuffer buffer = getBufferByte(getTotalLength(strings) + strings.length);

		final InfiniteCharSequence infiniteSeq = getInfiniteSeq();
		for ( int i = 0; i < strings.length; i++ ) {
			infiniteSeq.setString(strings[i]);
			encoder.encode(infiniteSeq.buffer, buffer, true);
			buffer.put((byte)0);
		}
		infiniteSeq.clear();

		buffer.flip();
		return buffer;
	}

	/**
	 * Returns a buffer containing the lengths of the specified strings.
	 *
	 * @param strings
	 *
	 * @return the String lengths in a PointerBuffer
	 */
	static PointerBuffer getLengths(final CharSequence[] strings) {
		PointerBuffer buffer = getLengths(strings.length);

		for ( int i = 0; i < strings.length; i++ )
			buffer.put(strings[i].length());

		buffer.flip();
		return buffer;
	}

	/**
	 * Returns a buffer containing the lengths of the specified buffers.
	 *
	 * @param buffers the buffer array
	 *
	 * @return the buffer lengths in a PointerBuffer
	 */
	static PointerBuffer getLengths(final ByteBuffer[] buffers) {
		PointerBuffer buffer = getLengths(buffers.length);

		for ( int i = 0; i < buffers.length; i++ )
			buffer.put(buffers[i].remaining());

		buffer.flip();
		return buffer;
	}

	static int getSize(final PointerBuffer lengths) {
		long size = 0;
		for ( int i = lengths.position(); i < lengths.limit(); i++ )
			size += lengths.get(i);

		return (int)size;
	}

	static void getClassTokens(final Class[] tokenClasses, final Map<Integer, String> target, final TokenFilter filter) {
		getClassTokens(Arrays.asList(tokenClasses), target, filter);
	}

	static void getClassTokens(final Iterable<Class> tokenClasses, final Map<Integer, String> target, final TokenFilter filter) {
		final int TOKEN_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

		for ( final Class tokenClass : tokenClasses ) {
			for ( final Field field : tokenClass.getDeclaredFields() ) {
				// Get only <public static final int> fields.
				if ( (field.getModifiers() & TOKEN_MODIFIERS) == TOKEN_MODIFIERS && field.getType() == int.class ) {
					try {
						final int value = field.getInt(null);
						if ( filter != null && !filter.accept(field, value) )
							continue;

						if ( target.containsKey(value) ) // Print colliding tokens in their hex representation.
							target.put(value, "0x" + Integer.toHexString(value).toUpperCase());
						else
							target.put(value, field.getName());
					} catch (IllegalAccessException e) {
						// Ignore
					}
				}
			}
		}
	}

	static ByteBuffer getNativeKernelArgs(final long user_func_ref, final CLMem[] clMems, final long[] sizes) {
		final ByteBuffer args = getBufferByte(8 + 4 + (clMems == null ? 0 : clMems.length * (4 + PointerBuffer.getPointerSize())));

		args.putLong(0, user_func_ref);
		if ( clMems == null )
			args.putInt(8, 0);
		else {
			args.putInt(8, clMems.length);
			int byteIndex = 12;
			for ( int i = 0; i < clMems.length; i++ ) {
				if ( LWJGLUtil.DEBUG && !clMems[i].isValid() )
					throw new IllegalArgumentException("An invalid CLMem object was specified.");
				args.putInt(byteIndex, (int)sizes[i]); // CLMem size
				byteIndex += (4 + PointerBuffer.getPointerSize()); // Skip size and make room for the pointer
			}
		}

		return args;
	}

	/**
	 * Releases all sub-devices created from the specified CLDevice.
	 *
	 * @param device the CLDevice to clear
	 */
	static void releaseObjects(final CLDevice device) {
		// Release objects only if we're about to hit 0.
		if ( device.getReferenceCount() > 1 )
			return;

		releaseObjects(device.getSubCLDeviceRegistry(), DESTRUCTOR_CLSubDevice);
	}

	/**
	 * Releases all objects contained in the specified CLContext.
	 *
	 * @param context the CLContext to clear
	 */
	static void releaseObjects(final CLContext context) {
		// Release objects only if we're about to hit 0.
		if ( context.getReferenceCount() > 1 )
			return;

		releaseObjects(context.getCLEventRegistry(), DESTRUCTOR_CLEvent);
		releaseObjects(context.getCLProgramRegistry(), DESTRUCTOR_CLProgram);
		releaseObjects(context.getCLSamplerRegistry(), DESTRUCTOR_CLSampler);
		releaseObjects(context.getCLMemRegistry(), DESTRUCTOR_CLMem);
		releaseObjects(context.getCLCommandQueueRegistry(), DESTRUCTOR_CLCommandQueue);
	}

	/**
	 * Releases all objects contained in the specified CLProgram.
	 *
	 * @param program the CLProgram to clear
	 */
	static void releaseObjects(final CLProgram program) {
		// Release objects only if we're about to hit 0.
		if ( program.getReferenceCount() > 1 )
			return;

		releaseObjects(program.getCLKernelRegistry(), DESTRUCTOR_CLKernel);
	}

	/**
	 * Releases all objects contained in the specified CLCommandQueue.
	 *
	 * @param queue the CLCommandQueue to clear
	 */
	static void releaseObjects(final CLCommandQueue queue) {
		// Release objects only if we're about to hit 0.
		if ( queue.getReferenceCount() > 1 )
			return;

		releaseObjects(queue.getCLEventRegistry(), DESTRUCTOR_CLEvent);
	}

	static Set<String> getExtensions(final String extensionList) {
		final Set<String> extensions = new HashSet<String>();

		final StringTokenizer tokenizer = new StringTokenizer(extensionList);
		while ( tokenizer.hasMoreTokens() )
			extensions.add(tokenizer.nextToken());

		return extensions;
	}

	private static <T extends CLObjectChild> void releaseObjects(final CLObjectRegistry<T> registry, final ObjectDestructor<T> destructor) {
		if ( registry.isEmpty() )
			return;

		for ( final T object : registry.getAll() ) {
			while ( object.isValid() )
				destructor.release(object);
		}
	}

	private interface ObjectDestructor<T extends CLObjectChild> {

		void release(T object);

	}

	/**
	 * A mutable CharSequence with very large initial length. We can wrap this in a re-usable CharBuffer for decoding.
	 * We cannot subclass CharBuffer because of {@link java.nio.CharBuffer#toString(int,int)}.
	 */
	private static class InfiniteCharSequence implements CharSequence {

		final CharBuffer buffer;

		CharSequence string;

		InfiniteCharSequence() {
			buffer = CharBuffer.wrap(this);
		}

		void setString(final CharSequence string) {
			this.string = string;
			this.buffer.position(0);
			this.buffer.limit(string.length());
		}

		void clear() {
			this.string = null;
		}

		public int length() {
			return Integer.MAX_VALUE;
		}

		public char charAt(final int index) {
			return string.charAt(index);
		}

		public CharSequence subSequence(final int start, final int end) {
			return string.subSequence(start, end);
		}
	}

	private static class Buffers {

		final ShortBuffer shorts;
		final IntBuffer ints;
		final IntBuffer intsDebug;
		final LongBuffer longs;

		final FloatBuffer floats;
		final DoubleBuffer doubles;

		final PointerBuffer pointers;

		Buffers() {
			shorts = BufferUtils.createShortBuffer(BUFFERS_SIZE);
			ints = BufferUtils.createIntBuffer(BUFFERS_SIZE);
			intsDebug = BufferUtils.createIntBuffer(1);
			longs = BufferUtils.createLongBuffer(BUFFERS_SIZE);

			floats = BufferUtils.createFloatBuffer(BUFFERS_SIZE);
			doubles = BufferUtils.createDoubleBuffer(BUFFERS_SIZE);

			pointers = BufferUtils.createPointerBuffer(BUFFERS_SIZE);
		}

	}

	/** Simple interface for Field filtering */
	interface TokenFilter {

		/** Should return true if the specified Field passes the filter. */
		boolean accept(Field field, int value);

	}

}