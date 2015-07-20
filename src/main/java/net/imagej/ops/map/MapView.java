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

package net.imagej.ops.map;

import net.imagej.ops.ComputerOp;
import net.imagej.ops.Input;
import net.imagej.ops.Output;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

/**
 * Abstract implementation of a {@link MapOp} which virtually converts entries in
 * I and V from A to B.
 *
 * @author Christian Dietz (University of Konstanz)
 * @param <A> type to be converted to <B>
 * @param <B> result of conversion
 * @param <I> holding <A>s
 * @param <O> type of resulting output
 */
public abstract class MapView<A, B, I, O> implements MapOp<A, B, ComputerOp<A, B>>,
	Input<I>, Output<O>
{

	@Parameter
	private I input;

	@Parameter
	private ComputerOp<A, B> op;

	@Parameter
	private B type;

	@Parameter(type = ItemIO.OUTPUT)
	private O output;

	@Override
	public ComputerOp<A, B> getOp() {
		return op;
	}

	@Override
	public void setOp(final ComputerOp<A, B> op) {
		this.op = op;
	}

	/**
	 * @return input which will be converted to a converted output
	 */
	@Override
	public I getInput() {
		return input;
	}

	/**
	 * @param input which will be converted to a converted output
	 */
	@Override
	public void setInput(final I input) {
		this.input = input;
	}

	/**
	 * Set the resulting output
	 *
	 * @param output
	 */
	@Override
	public void setOutput(final O output) {
		this.output = output;
	}

	/**
	 * @return the resulting converted output
	 */
	@Override
	public O getOutput() {
		return output;
	}

	/**
	 * @return type of resulting converted output
	 */
	public B getType() {
		return type;
	}

	/**
	 * @param type of resulting converted output
	 */
	public void setType(final B type) {
		this.type = type;
	}
}
