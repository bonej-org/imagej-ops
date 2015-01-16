/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ops.onthefly;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import net.imagej.ops.Contingent;
import net.imagej.ops.Op;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * A class generator for specialized {@link Op}s.
 * <p>
 * This class uses <a href="http://www.javassist.org/">Javassist</a> to generate
 * optimized versions of arithmetic operations on-the-fly. To this end, the
 * {@link #conforms()} method has to match the parameters carefully, to ensure
 * that the {@link #run()} method will not have problems processing those
 * parameters.
 * </p>
 * <p>
 * When {@link #run()} is called, it first determines the exact data types of
 * its parameters -- verifying again that the op works for them -- and then
 * generates a new class, based on the exact parameter types, which it loads
 * into a new class loader -- to avoid polluting the current class loader -- and
 * executes it. To avoid unnecessary work, the generated classes are cached.
 * </p>
 * 
 * @author Johannes Schindelin
 */
public abstract class ArithmeticOp implements Op, Contingent {

	/** The 'add' op */
	@Plugin(type = Op.class, name = "add", priority = Priority.HIGH_PRIORITY)
	public static class AddOp extends ArithmeticOp {

		@Override
		public void run() {
			run("add", "+");
		}
	}

	/** The 'subtract' op */
	@Plugin(type = Op.class, name = "subtract", priority = Priority.HIGH_PRIORITY)
	public static class SubtractOp extends ArithmeticOp {

		@Override
		public void run() {
			run("subtract", "-");
		}
	}

	/** The 'multiply' op */
	@Plugin(type = Op.class, name = "multiply", priority = Priority.HIGH_PRIORITY)
	public static class MultiplyOp extends ArithmeticOp {

		@Override
		public void run() {
			run("multiply", "*");
		}
	}

	/** The 'divide' op */
	@Plugin(type = Op.class, name = "divide", priority = Priority.HIGH_PRIORITY)
	public static class DivideOp extends ArithmeticOp {

		@Override
		public void run() {
			run("divide", "/");
		}
	}

	@Parameter(type = ItemIO.BOTH)
	protected Object result;

	@Parameter
	protected Object a;

	@Parameter
	protected Object b;

	/**
	 * A generic interface to be implemented by the classes generated by this
	 * class.
	 */
	public interface MyOp {

		/**
		 * The most generic signature for an operation on two inputs with one
		 * output.
		 * 
		 * @param result output
		 * @param a first input
		 * @param b second input
		 */
		void run(Object result, Object a, Object b);
	}

	protected void run(final String name, final String operator) {
		final MyOp op = findMyOp(name, operator, result, a, b);
		if (op == null) throw new UnsupportedOperationException("name/operator: " + name + ", " + operator);
		op.run(result, a, b);
	}

	/**
	 * Generates an op matching the given parameters.
	 *  
	 * @param name the name of the {@link Op}
	 * @param operator the Java operator performing the {@link Op} on primitive types
	 * @param result the result object
	 * @param a the first operand
	 * @param b the second operand
	 * @return the generated {@link Op}, or null
	 */
	public static Op findOp(final String name, final String operator,
		final Object result, final Object a, final Object b)
	{
		final MyOp myOp = findMyOp(name, operator, result, a, b);
		if (myOp == null) return null;
		final ArithmeticOp op = new ArithmeticOp() {

			@Override
			public void run() {
				myOp.run(result, a, b);
			}
		};
		op.result = result;
		op.a = a;
		op.b = b;
		return op;
	}

	private static MyOp findMyOp(final String name, final String operator, final Object result, final Object a, final Object b) {
		if (result == b && a != b) return null;
		if (a instanceof ArrayImg) {
			final Object access = ((ArrayImg<?, ?>) a).update(null);
			if (access instanceof ArrayDataAccess) {
				final Object a2 = ((ArrayDataAccess<?>) access).getCurrentStorageArray();
				if (a2 == null || !a2.getClass().isArray() || !a2.getClass().getComponentType().isPrimitive()) return null;

				if (result == null || !(result instanceof ArrayImg)) return null;
				if (!dimensionsMatch((ArrayImg<?, ?>) a, (ArrayImg<?, ?>) result)) return null;
				final Object resultAccess = ((ArrayImg<?, ?>) result).update(null);
				if (!(resultAccess instanceof ArrayDataAccess)) return null;
				final Object result2 = ((ArrayDataAccess<?>) resultAccess).getCurrentStorageArray();
				if (result2 == null || result2.getClass() != a2.getClass()) return null;

				if (b instanceof RealType) {
					return getMyConstantOp(a2.getClass(), name, operator);
				}
				if (b instanceof ArrayImg) {
					if (!dimensionsMatch((ArrayImg<?, ?>) a, (ArrayImg<?, ?>) b)) return null;
					final Object bAccess = ((ArrayImg<?, ?>) b).update(null);
					if (!(bAccess instanceof ArrayDataAccess)) return null;
					final Object b2 = ((ArrayDataAccess<?>) bAccess).getCurrentStorageArray();
					if (b2 == null || b2.getClass() != a2.getClass()) return null;
					return getMyOp(a2.getClass(), name, operator);
				}
			}
		}
		if (a instanceof PlanarImg) {
			final PlanarImg<?, ?> a2 = (PlanarImg<?, ?>) a;
			if (a2.numSlices() == 0) return null;
			final Object plane =
				((ArrayDataAccess<?>) a2.getPlane(0)).getCurrentStorageArray();
			if (plane == null || !plane.getClass().isArray() || !plane.getClass().getComponentType().isPrimitive()) return null;
			if (!(b instanceof PlanarImg) || !(result instanceof PlanarImg)) return null;
			final PlanarImg<?, ?> b2 = (PlanarImg<?, ?>) b;
			if (!dimensionsMatch(a2, b2)) return null;
			final PlanarImg<?, ?> result2 = (PlanarImg<?, ?>) result;
			if (!dimensionsMatch(a2, result2)) return null;
			final int numSlices = a2.numSlices();
			if (numSlices == 0 || numSlices != b2.numSlices() ||
				numSlices != result2.numSlices()) return null;
			final Object aData = a2.getPlane(0);
			if (!(aData instanceof ArrayDataAccess)) return null;
			final Object bData = b2.getPlane(0);
			if (aData.getClass() != bData.getClass()) return null;
			final Object resultData = result2.getPlane(0);
			if (aData.getClass() != resultData.getClass()) return null;
			return getPlanarOp(plane.getClass(), name, operator);
		}
		return null;
	}

	@Override
	public boolean conforms() {
		return findMyOp("add", "+", result, a, b) != null;
	}

	private static boolean dimensionsMatch(final Img<?> aImg, final Img<?> bImg) {
		final int numDimensions = aImg.numDimensions();
		if (numDimensions != bImg.numDimensions()) return false;
		for (int i = 0; i < numDimensions; i++) {
			if (aImg.dimension(i) != bImg.dimension(i)) return false;
		}
		return true;
	}

	private final static Map<String, MyOp> ops = new HashMap<String, MyOp>();
	private final static ClassLoader loader;
	private final static ClassPool pool;

	static {
		loader = new URLClassLoader(new URL[0]);
		pool = new ClassPool(false);
		pool.appendClassPath(new ClassClassPath(AddOp.class));
	}

	private static MyOp getMyOp(final Class<?> forClass, final String name,
		final String operator)
	{
		final String componentType = forClass.getComponentType().getSimpleName();
		final String myOpName = "myOp$" + name + "$" + componentType;
		MyOp op = ops.get(myOpName);
		if (op != null) return op;

		try {
			final String type = forClass.getSimpleName();
			final CtClass clazz =
				pool.makeClass(myOpName, pool.get(Object.class.getName()));
			clazz.addInterface(pool.get(MyOp.class.getName()));
			final String src =
				replace(makeRun(castArrayImg(type, "a", "b", "result"),
					"for (int i = 0; i < a2.length; i++) {",
					"  result2[i] = (CTYPE) (a2[i] OP b2[i]);", // actual op
					"}"), "OP", operator, "CTYPE", componentType, "TYPE", type);
			clazz.addMethod(CtNewMethod.make(src, clazz));
			op = (MyOp) clazz.toClass(loader, null).newInstance();
			ops.put(myOpName, op);
			return op;
		}
		catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private static MyOp getMyConstantOp(final Class<?> forClass, final String name,
		final String operator)
	{
		final String componentType = forClass.getComponentType().getSimpleName();
		final String myOpName = "myConstantOp$" + name + "$" + componentType;
		MyOp op = ops.get(myOpName);
		if (op != null) return op;

		try {
			final String type = forClass.getSimpleName();
			final CtClass clazz =
				pool.makeClass(myOpName, pool.get(Object.class.getName()));
			clazz.addInterface(pool.get(MyOp.class.getName()));
			final String src =
				replace(makeRun(castArrayImg(type, "a", "result"), // a & result are ArrayImgs
					castRealType(componentType, "b"), // b is a constant
					"for (int i = 0; i < a2.length; i++) {",
					"  result2[i] = (CTYPE) (a2[i] OP b2);", // actual op
					"}"), "OP", operator, "CTYPE", componentType, "TYPE", type);
			clazz.addMethod(CtNewMethod.make(src, clazz));
			op = (MyOp) clazz.toClass(loader, null).newInstance();
			ops.put(myOpName, op);
			return op;
		}
		catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private static MyOp getPlanarOp(final Class<?> forClass, final String name,
		final String operator)
	{
		final String componentType = forClass.getComponentType().getSimpleName();
		final String myOpName = "myOp$planar$" + name + "$" + componentType;
		MyOp op = ops.get(myOpName);
		if (op != null) return op;

		try {
			final String imgType = PlanarImg.class.getName();
			final String type = forClass.getSimpleName();
			final CtClass clazz =
				pool.makeClass(myOpName, pool.get(Object.class.getName()));
			clazz.addInterface(pool.get(MyOp.class.getName()));
			final String src =
				replace(
					makeRun(
						castPlanarImg("a", "b", "result"),
						"for (int j = 0; j < a2.numSlices(); j++) {",
						"  TYPE a3 = (TYPE) a2.getPlane(j).getCurrentStorageArray();",
						"  TYPE b3 = (TYPE) b2.getPlane(j).getCurrentStorageArray();",
						"  TYPE result3 = (TYPE) result2.getPlane(j).getCurrentStorageArray();",
						"  for (int i = 0; i < a3.length; i++) {",
						"    result3[i] = (CTYPE) (a3[i] OP b3[i]);", // actual op
						"  }", // inner loop
						"}"), "OP", operator, "ITYPE", imgType, "CTYPE", componentType,
					"TYPE", type);
			clazz.addMethod(CtNewMethod.make(src, clazz));
			op = (MyOp) clazz.toClass(loader, null).newInstance();
			ops.put(myOpName, op);
			return op;
		}
		catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private static String
		castArrayImg(final String toType, final String... names)
	{
		final StringBuilder builder = new StringBuilder();
		for (final String name : names) {
			final String toImg = "((" + ArrayImg.class.getName() + ") " + name + ")";
			final String toArrayDataAccess =
				"((" + ArrayDataAccess.class.getName() + ") " + toImg +
					".update(null))";
			builder.append(toType + " " + name + "2 = (" + toType + ") " +
				toArrayDataAccess + ".getCurrentStorageArray();\n");
		}
		return builder.toString();
	}

	private static String
		castRealType(final String toType, final String... names)
	{
		final StringBuilder builder = new StringBuilder();
		for (final String name : names) {
			final String toRealType = "(" + RealType.class.getName() + ") " + name;
			final String toDouble = "(" + toRealType + ").getRealDouble()";
			builder.append(toType + " " + name + "2 = (" + toType + ") " + toDouble +
				";\n");
		}
		return builder.toString();
	}

	private static String
	castPlanarImg(final String... names)
{
	final StringBuilder builder = new StringBuilder();
	final String toType = PlanarImg.class.getName();
	for (final String name : names) {
		builder.append(toType + " " + name + "2 = (" + toType + ") " + name + ";\n");
	}
	return builder.toString();
}

	private static String makeRun(final String... body)
	{
		final StringBuilder builder = new StringBuilder();
		builder
			.append("public void run(java.lang.Object result, java.lang.Object a, java.lang.Object b) {\n");
		for (final String line : body) {
			builder.append("  ").append(line).append("\n");
		}
		builder.append("}\n");
		return builder.toString();
	}

	private static String replace(final String template, final String... pairs) {
		if ((pairs.length % 2) != 0) {
			throw new RuntimeException("Invalid number of arguments: " + pairs.length);
		}
		String result = template;
		for (int i = 0; i < pairs.length; i += 2) {
			result = result.replace(pairs[i], pairs[i + 1]);
		}
		return result;
	}
}
