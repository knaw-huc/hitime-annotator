package nl.knaw.huc.hitimepmerger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class ItemDto {
  public String id;
  public String input;
  public List<CandidateDto> candidates;
  public String golden;
  public ItemType type;
  public Boolean controlaccess;
  public String method;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CandidateDto {
  public String id;
  public List<String> names;
  public int distance;
}

