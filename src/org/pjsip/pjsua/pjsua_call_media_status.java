/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua;

public enum pjsua_call_media_status {
  PJSUA_CALL_MEDIA_NONE,
  PJSUA_CALL_MEDIA_ACTIVE,
  PJSUA_CALL_MEDIA_LOCAL_HOLD,
  PJSUA_CALL_MEDIA_REMOTE_HOLD,
  PJSUA_CALL_MEDIA_ERROR;

  public final int swigValue() {
    return swigValue;
  }

  public static pjsua_call_media_status swigToEnum(int swigValue) {
    pjsua_call_media_status[] swigValues = pjsua_call_media_status.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (pjsua_call_media_status swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + pjsua_call_media_status.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private pjsua_call_media_status() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private pjsua_call_media_status(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private pjsua_call_media_status(pjsua_call_media_status swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

