package ox.util;

import java.time.LocalDate;

import ox.Money;
import ox.x.XList;

public interface CSVRowReader {

  public String get(String s);

  public LocalDate getDate(String colName);

  public LocalDate getISODate(String colName);

  public LocalDate getExcelDate(String colName);

  public LocalDate getParsedDate(String colName);

  public Money getMoney(String colName);

  public Integer getInt(String colName);

  public Long getLong(String colName);

  public Double getDouble(String colName);

  public <T extends Enum<T>> T getEnum(String colName, Class<T> enumType);

  public Boolean getBoolean(String colName);

  public XList<String> asList();

}
