package nl.knaw.huc.hitimepmerger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static org.apache.commons.lang3.StringUtils.isBlank;

enum ItemType {

  GEOG("N.A.", "N.A.", "Geographic Names", "Geografische namen"),
  CORP("corp", "610$a", "Organizations", "Organisaties"),
  PERS("pers", "600$a", "Persons", "Personen");

  private final String type;

  private final String encodinganalog;
  private final String headEng;
  private final String headDut;

  ItemType(String type, String encodinganalog, String headEng, String headDut) {
    this.type = type;
    this.encodinganalog = encodinganalog;
    this.headEng = headEng;
    this.headDut = headDut;
  }

  @JsonCreator
  public static ItemType fromString(String key) {
    if (isBlank(key)) {
      throw new IllegalArgumentException("key is blank");
    }
    return ItemType.valueOf(key.toUpperCase());
  }

  @JsonValue
  public String getType() {
    return type;
  }

  public String getEncodinganalog() {
    return encodinganalog;
  }

  public String getElementName() {
    return this.type + "name";
  }

  public String getHeadEng() {
    return headEng;
  }

  public String getHeadDut() {
    return headDut;
  }

}
