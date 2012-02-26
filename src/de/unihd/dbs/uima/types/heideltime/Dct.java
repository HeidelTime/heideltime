

/* First created by JCasGen Sat Apr 30 11:35:10 CEST 2011 */
package de.unihd.dbs.uima.types.heideltime;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed May 04 16:11:59 CEST 2011
 * XML source: /home/jstroetgen/workspace/heideltime-kit/desc/reader/ACETernReader.xml
 * @generated */
public class Dct extends Annotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(Dct.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Dct() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Dct(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Dct(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Dct(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {}
     
 
    
  //*--------------*
  //* Feature: filename

  /** getter for filename - gets 
   * @generated */
  public String getFilename() {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_filename == null)
      jcasType.jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Dct");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dct_Type)jcasType).casFeatCode_filename);}
    
  /** setter for filename - sets  
   * @generated */
  public void setFilename(String v) {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_filename == null)
      jcasType.jcas.throwFeatMissing("filename", "de.unihd.dbs.uima.types.heideltime.Dct");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dct_Type)jcasType).casFeatCode_filename, v);}    
   
    
  //*--------------*
  //* Feature: value

  /** getter for value - gets 
   * @generated */
  public String getValue() {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "de.unihd.dbs.uima.types.heideltime.Dct");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dct_Type)jcasType).casFeatCode_value);}
    
  /** setter for value - sets  
   * @generated */
  public void setValue(String v) {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "de.unihd.dbs.uima.types.heideltime.Dct");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dct_Type)jcasType).casFeatCode_value, v);}    
   
    
  //*--------------*
  //* Feature: timexId

  /** getter for timexId - gets 
   * @generated */
  public String getTimexId() {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_timexId == null)
      jcasType.jcas.throwFeatMissing("timexId", "de.unihd.dbs.uima.types.heideltime.Dct");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Dct_Type)jcasType).casFeatCode_timexId);}
    
  /** setter for timexId - sets  
   * @generated */
  public void setTimexId(String v) {
    if (Dct_Type.featOkTst && ((Dct_Type)jcasType).casFeat_timexId == null)
      jcasType.jcas.throwFeatMissing("timexId", "de.unihd.dbs.uima.types.heideltime.Dct");
    jcasType.ll_cas.ll_setStringValue(addr, ((Dct_Type)jcasType).casFeatCode_timexId, v);}    
  }

    