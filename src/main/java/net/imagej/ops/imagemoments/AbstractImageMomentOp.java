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

package net.imagej.ops.imagemoments;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

import net.imagej.ops.AbstractHybridOp;
import net.imagej.ops.Contingent;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Abstract {@link ImageMomentOp}. Provides {@link OpService} and create the
 * output.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 * @author Christian Dietz, University of Konstanz.
 * @param <I> input type
 * @param <O> output type
 */
public abstract class AbstractImageMomentOp<I extends RealType<I>, O extends RealType<O>>
	extends AbstractHybridOp<IterableInterval<I>, O> implements
	ImageMomentOp<IterableInterval<I>, O>, Contingent
{

	@Parameter(type = ItemIO.INPUT)
	protected OpService ops;

	@SuppressWarnings("unchecked")
	@Override
	public O createOutput(IterableInterval<I> input) {
		return (O) new DoubleType();
	}

	@Override
	public boolean conforms() {
		return 2 == getInput().numDimensions();
	}
}