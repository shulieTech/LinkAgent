/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.shulie.instrument.simulator.core.ignore;

public interface IgnoredTypesConfigurer{

  /**
   * Configure the passed {@code builder} and define which classes should be ignored when
   * instrumenting.
   */
  void configure(IgnoredTypesBuilder builder);

   int order();
}
