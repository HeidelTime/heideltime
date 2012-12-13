

/* First created by JCasGen Thu Sep 20 15:38:14 CEST 2012 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Thu Sep 20 15:38:14 CEST 2012
 * XML source: /home/julian/heideltime/heideltime-kit/desc/type/HeidelTime_TypeSystem.xml
 * @generated */
public class Timex3Interval extends Timex3 {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(Timex3Interval.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Timex3Interval() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Timex3Interval(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Timex3Interval(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Timex3Interval(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: TimexValueEB

  /** getter for TimexValueEB - gets 
   * @generated */
  public String getTimexValueEB() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEB == null)
      jcasType.jcas.throwFeatMissing("TimexValueEB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEB);}
    
  /** setter for TimexValueEB - sets  
   * @generated */
  public void setTimexValueEB(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEB == null)
      jcasType.jcas.throwFeatMissing("TimexValueEB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEB, v);}    
   
    
  //*--------------*
  //* Feature: TimexValueLE

  /** getter for TimexValueLE - gets 
   * @generated */
  public String getTimexValueLE() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLE == null)
      jcasType.jcas.throwFeatMissing("TimexValueLE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLE);}
    
  /** setter for TimexValueLE - sets  
   * @generated */
  public void setTimexValueLE(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLE == null)
      jcasType.jcas.throwFeatMissing("TimexValueLE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLE, v);}    
   
    
  //*--------------*
  //* Feature: TimexValueEE

  /** getter for TimexValueEE - gets 
   * @generated */
  public String getTimexValueEE() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEE == null)
      jcasType.jcas.throwFeatMissing("TimexValueEE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEE);}
    
  /** setter for TimexValueEE - sets  
   * @generated */
  public void setTimexValueEE(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueEE == null)
      jcasType.jcas.throwFeatMissing("TimexValueEE", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueEE, v);}    
   
    
  //*--------------*
  //* Feature: TimexValueLB

  /** getter for TimexValueLB - gets 
   * @generated */
  public String getTimexValueLB() {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLB == null)
      jcasType.jcas.throwFeatMissing("TimexValueLB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLB);}
    
  /** setter for TimexValueLB - sets  
   * @generated */
  public void setTimexValueLB(String v) {
    if (Timex3Interval_Type.featOkTst && ((Timex3Interval_Type)jcasType).casFeat_TimexValueLB == null)
      jcasType.jcas.throwFeatMissing("TimexValueLB", "de.unihd.dbs.uima.types.heideltime.Timex3Interval");
    jcasType.ll_cas.ll_setStringValue(addr, ((Timex3Interval_Type)jcasType).casFeatCode_TimexValueLB, v);}    
  }

    