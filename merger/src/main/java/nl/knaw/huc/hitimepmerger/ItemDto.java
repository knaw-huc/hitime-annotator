package nl.knaw.huc.hitimepmerger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
class ItemDto {
  public String id;
  public String input;
  public List<CandidateDto> candidates;
  public String golden;
  public ItemTypeDto type;
  public Boolean controlaccess;
  public String method;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CandidateDto {
  public String id;
  public List<String> names;
  public int distance;
}

enum ItemTypeDto {

  CORP("corp", "610$a"),
  PERS("pers", "600$a");

  private final String type;

  private final String encodinganalog;

  ItemTypeDto(String type, String encodinganalog) {
    this.type = type;
    this.encodinganalog = encodinganalog;
  }

  @JsonCreator
  public static ItemTypeDto fromString(String key) {
    if (isBlank(key)) {
      throw new IllegalArgumentException("key is blank");
    }
    return ItemTypeDto.valueOf(key.toUpperCase());
  }

  @JsonValue
  public String getType() {
    return type;
  }

  public String getEncodinganalog() {
    return encodinganalog;
  }
}