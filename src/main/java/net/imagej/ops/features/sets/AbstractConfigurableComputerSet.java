/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
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

package net.imagej.ops.features.sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.imagej.ops.CustomOpEnvironment;
import net.imagej.ops.Op;
import net.imagej.ops.OpEnvironment;
import net.imagej.ops.OpInfo;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imglib2.type.Type;

import org.scijava.plugin.Parameter;

/**
 * An abstract implementation of {@link ComputerSet} which is configurable.
 *
 * {@link Computers} passed by construction will be activated and all remaining
 * will be deactivated. If no {@link Computers} is passed, all {@link Computers}
 * will be activated by default.
 *
 * @author Tim-Oliver Buchholz, University of Konstanz
 *
 * @param <I>
 *            type of the common input
 * @param <O>
 *            type of the common output
 */
public abstract class AbstractConfigurableComputerSet<I, O extends Type<O>> extends AbstractComputerSet<I, O>
		implements ConfigurableComputerSet<I, O> {

	@Parameter(required = false)
	private List<Class<? extends Op>> active;

	/**
	 * The activated {@link Computers}.
	 */
	private final Map<Class<? extends Op>, Boolean> activated;

	/**
	 * Create a new {@link AbstractConfigurableComputerSet} with the default
	 * {@link OpEnvironment}.
	 *
	 * @param outputTypeInstance
	 *            object of the output type
	 * @param inType
	 *            the input type
	 */
	public AbstractConfigurableComputerSet(final O outputTypeInstance, final Class<I> inType) {
		super(null, outputTypeInstance, inType);
		active = new ArrayList<>();
		activated = new HashMap<>();
	}

	/**
	 * Create a new {@link AbstractConfigurableComputerSet} with a custom
	 * {@link OpEnvironment}.
	 *
	 * @param opEnv
	 *            the custom {@link OpEnvironment}
	 * @param outputTypeInstance
	 *            object of the output type
	 * @param inType
	 *            the input type
	 */
	public AbstractConfigurableComputerSet(final OpEnvironment opEnv, final O outputTypeInstance,
			final Class<I> inType) {
		super(opEnv, outputTypeInstance, inType);
		active = new ArrayList<>();
		activated = new HashMap<>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize() {
		if (active.isEmpty()) {
			active.addAll(Arrays.asList(getFeatures()));
		}
		super.initialize();
	}

	/**
	 * Set {@link CustomOpEnvironment} and create all computers of this
	 * featureset and activate passed {@link Computers}.
	 *
	 * @param infos
	 *            for the {@link CustomOpEnvironment}
	 */
	@Override
	protected void initialize(final Collection<OpInfo> infos) {
		if (infos != null) {
			setEnvironment(new CustomOpEnvironment(ops(), infos));
		}

		for (final Class<? extends Op> feature : getFeatures()) {
			if (active.contains(feature)) {
				activated.put(feature, true);
				addComputer(feature);
			} else {
				activated.put(feature, false);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, O> compute1(final I input) {
		for (final UnaryComputerOp<I, O> computer : activated.entrySet().stream().filter(a -> a.getValue())
				.map(a -> computers.get(a.getKey())).collect(Collectors.toList())) {
			computer.compute1(input, computer.out());
		}

		return namedOutputs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isActive(final Class<? extends Op> feature) {
		return activated.get(feature);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Class<? extends Op>> getActive() {
		return activated.entrySet().stream().filter(e -> e.getValue()).map(e -> e.getKey())
				.collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends Op>[] getComputedFeatures() {
		return getActive().toArray(new Class[(int) activated.values().stream().filter(a -> a.booleanValue()).count()]);
	}

	@Override
	public String[] getComputerNames() {
		return getActive().stream().map(a -> a.getSimpleName()).collect(Collectors.toList())
				.toArray(new String[computers.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Class<? extends Op>> getInactive() {
		return activated.entrySet().stream().filter(e -> !e.getValue()).map(e -> e.getKey())
				.collect(Collectors.toList());
	}
}