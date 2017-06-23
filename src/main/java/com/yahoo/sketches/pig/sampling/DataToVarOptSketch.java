/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.pig.sampling;

import static com.yahoo.sketches.pig.sampling.VarOptCommonImpl.DEFAULT_TARGET_K;
import static com.yahoo.sketches.pig.sampling.VarOptCommonImpl.DEFAULT_WEIGHT_IDX;

import java.io.IOException;

import org.apache.pig.AccumulatorEvalFunc;
import org.apache.pig.Algebraic;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;

import com.yahoo.sketches.sampling.VarOptItemsSketch;

/**
 * Creates a binary version of a VarOpt sampling over input tuples. The resulting
 * <tt>DataByteArray</tt> can be read in Pig with <tt>GetVarOptSamples</tt>, although the
 * per-record schema will be lost. It implements both the <tt>Accumulator</tt> and
 * <tt>Algebraic</tt> interfaces for efficient performance.
 *
 * @author Jon Malkin
 */
public class DataToVarOptSketch extends AccumulatorEvalFunc<DataByteArray> implements Algebraic {
  private static final ArrayOfTuplesSerDe serDe_ = new ArrayOfTuplesSerDe();

  private final int targetK_;
  private final int weightIdx_;
  private VarOptItemsSketch<Tuple> sketch_;

  /**
   * VarOpt sampling constructor.
   * @param kStr String indicating the maximum number of desired entries in the reservoir.
   */
  public DataToVarOptSketch(final String kStr) {
    targetK_ = Integer.parseInt(kStr);
    weightIdx_ = DEFAULT_WEIGHT_IDX;

    if (targetK_ < 1) {
      throw new IllegalArgumentException("DataToVarOptSketch requires target sample size >= 1: "
              + targetK_);
    }
  }

  /**
   * VarOpt sampling constructor.
   * @param kStr String indicating the maximum number of desired samples to return.
   * @param weightIdxStr String indicating column index (0-based) of weight values
   */
  public DataToVarOptSketch(final String kStr, final String weightIdxStr) {
    targetK_ = Integer.parseInt(kStr);
    weightIdx_ = Integer.parseInt(weightIdxStr);

    if (targetK_ < 1) {
      throw new IllegalArgumentException("DataToVarOptSketch requires target sample size >= 1: "
              + targetK_);
    }
    if (weightIdx_ < 0) {
      throw new IllegalArgumentException("DataToVarOptSketch requires weight index >= 0: "
              + weightIdx_);
    }
  }

  DataToVarOptSketch() {
    targetK_ = DEFAULT_TARGET_K;
    weightIdx_ = DEFAULT_WEIGHT_IDX;
  }

  @Override
  public void accumulate(final Tuple inputTuple) throws IOException {
    if (inputTuple == null || inputTuple.size() < 1 || inputTuple.isNull(0)) {
      return;
    }

    final DataBag samples = (DataBag) inputTuple.get(0);

    if (sketch_ == null) {
      sketch_ = VarOptItemsSketch.newInstance(targetK_);
    }

    for (Tuple t : samples) {
      // first element is weight
      final double weight = (double) t.get(weightIdx_);
      sketch_.update(t, weight);
    }
  }

  @Override
  public DataByteArray getValue() {
    if (sketch_ == null) {
      return null;
    }

    return new DataByteArray(sketch_.toByteArray(serDe_));
  }

  @Override
  public void cleanup() {
    sketch_ = null;
  }


  @Override
  public String getInitial() {
    return VarOptCommonImpl.RawTuplesToSketchTuple.class.getName();
  }

  @Override
  public String getIntermed() {
    return VarOptCommonImpl.UnionSketchesAsTuple.class.getName();
  }

  @Override
  public String getFinal() {
    return VarOptCommonImpl.UnionSketchesAsByteArray.class.getName();
  }

  @Override
  public Schema outputSchema(final Schema input) {
    try {
      if (input == null || input.size() == 0) {
        throw new IllegalArgumentException("Degenerate input schema to VarOptSampling");
      }

      // first element must be a bag, first element of tuples must be the weight (float or double)
      if (input.getField(0).type != DataType.BAG) {
        throw new IllegalArgumentException("VarOpt input must be a data bag: "
                + input.toString());
      }

      final Schema record = input.getField(0).schema; // record has a tuple in field 0
      final Schema fields = record.getField(weightIdx_).schema; //
      if (fields.getField(weightIdx_).type != DataType.DOUBLE
              && fields.getField(0).type != DataType.FLOAT) {
        throw new IllegalArgumentException("weightIndex item of VarOpt tuple must be a "
                + "weight (double/float), found " + fields.getField(0).type
                + ": " + fields.toString());
      }

      return new Schema(new Schema.FieldSchema(getSchemaName(this
              .getClass().getName().toLowerCase(), input), DataType.BYTEARRAY));
    }
    catch (final FrontendException e) {
      return null;
    }
  }
}
