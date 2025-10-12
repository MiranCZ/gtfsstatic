package io.github.mirancz.gtfsparser.parsing;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.Iterator;

public class CalendarParser extends Parser {


    public CalendarParser() {
        subscribeTransformer("calendar.txt", "calendar", this::parseAndWriteCalendar);
        subscribeTransformer("calendar_dates.txt", "calendar_dates", this::parseAndWriteDates);
    }

    protected void parseAndWriteDates(InputStream input, DataOutputStream output) throws Exception {
        Csv entries = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = entries.getLines();


        while (lines.hasNext()) {
            output.writeBoolean(true);
            Csv.CsvLine line = lines.next();

            int serviceId = line.getInt("service_id");
            String date = line.get("date");

            int type = line.getInt("exception_type");

            if (serviceId > Short.MAX_VALUE || type > Byte.MAX_VALUE) {
                throw new IllegalStateException("Calendar date overflow! "+serviceId + " ; "+type);
            }

            output.writeShort(serviceId);
            output.writeInt(parseDate(date));
            output.writeByte(type);
        }

        output.writeBoolean(false);
    }

    protected void parseAndWriteCalendar(InputStream input, DataOutputStream output) throws Exception {
        Csv entries = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = entries.getLines();


        while (lines.hasNext()) {
            output.writeBoolean(true);
            Csv.CsvLine line = lines.next();

            int serviceId = line.getInt("service_id");

            String startDate = line.get("start_date");
            String endDate = line.get("end_date");

            boolean monday = line.getBoolean("monday");
            boolean tuesday = line.getBoolean("tuesday");
            boolean wednesday = line.getBoolean("wednesday");
            boolean thursday = line.getBoolean("thursday");
            boolean friday = line.getBoolean("friday");
            boolean saturday = line.getBoolean("saturday");
            boolean sunday = line.getBoolean("sunday");

            byte data = 0;
            if (monday)    data |= 1 << 0;
            if (tuesday)   data |= 1 << 1;
            if (wednesday) data |= 1 << 2;
            if (thursday)  data |= 1 << 3;
            if (friday)    data |= 1 << 4;
            if (saturday)  data |= 1 << 5;
            if (sunday)    data |= 1 << 6;

            if (serviceId > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Service id overflow! "+serviceId);
            }

            output.writeShort(serviceId);
            output.writeInt(parseDate(startDate));
            output.writeInt(parseDate(endDate));
            output.write(data);
        }

        output.writeBoolean(false);
    }

    private static int parseDate(String date) {
        int day = Integer.parseInt(date.substring(date.length()-2));
        int month = Integer.parseInt(date.substring(date.length()-4, date.length()-2));

        int year = Integer.parseInt(date.substring(0, 4));


        if (day > Byte.MAX_VALUE || month > Byte.MAX_VALUE || year > Short.MAX_VALUE) {
            throw new IllegalArgumentException("date overflow "+date);
        }

        return (year << 16) | (month<<8) | day;
    }

}
