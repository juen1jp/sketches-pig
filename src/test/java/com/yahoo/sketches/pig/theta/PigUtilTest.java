/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.pig.theta;

import static com.yahoo.sketches.pig.theta.PigUtil.compactOrderedSketchToTuple;
import static com.yahoo.sketches.pig.theta.PigUtil.extractTypeAtIndex;
import static org.testng.Assert.assertNull;

import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.testng.annotations.Test;

import com.yahoo.sketches.theta.CompactSketch;
import com.yahoo.sketches.theta.UpdateSketch;

/**
 * @author Lee Rhodes
 */
public class PigUtilTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkCompOrdSketchToTuple() {
    UpdateSketch usk = UpdateSketch.builder().setNominalEntries(16).build();
    for (int i=0; i<16; i++) usk.update(i);
    CompactSketch csk = usk.compact(false, null);
    compactOrderedSketchToTuple(csk);
  }
  
  @Test
  public void checkExtractTypeAtIndex() {
    Tuple tuple = TupleFactory.getInstance().newTuple(0);
    assertNull(extractTypeAtIndex(tuple, 0));
  }
  
  @Test
  public void printlnTest() {
    println(this.getClass().getSimpleName());
  }
  
  /**
   * @param s value to print 
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }
  
}
