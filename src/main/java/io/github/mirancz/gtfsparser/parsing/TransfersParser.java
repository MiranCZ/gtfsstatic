package io.github.mirancz.gtfsparser.parsing;

import io.github.mirancz.gtfsparser.util.CheckedOutputStream;
import io.github.mirancz.gtfsparser.util.IdStorage;
import io.github.mirancz.gtfsparser.util.StopInfo;
import io.github.mirancz.gtfsparser.util.Utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransfersParser extends Parser {

    public TransfersParser() {
        subscribeTransformer("transfers.txt", "transfers", this::parseAndWrite);
    }

    protected void parseAndWrite(InputStream input, CheckedOutputStream output) throws Exception {
        Csv entries = Csv.parse(input);

        Iterator<Csv.CsvLine> lines = entries.getLines();

        List<Transfer> transfers = new ArrayList<>();
        List<TripTransfer> tripTransfers = new ArrayList<>();

        while (lines.hasNext()) {
            Csv.CsvLine line = lines.next();

            StopInfo fromStop = Utils.parseStop(line.get("from_stop_id"));
            StopInfo toStop = Utils.parseStop(line.get("to_stop_id"));

            int transferType = line.getInt("transfer_type");
            int minTransferTime = line.getInt("min_transfer_time");

            if (line.getIntOrDefault("from_trip_id", -1) != -1) {
                int fromTripId = IdStorage.TRIP.getId(line.getInt("from_trip_id"));
                int toTripId = IdStorage.TRIP.getId(line.getInt("to_trip_id"));
                int maxWaitingTime = line.getInt("max_waiting_time");

                tripTransfers.add(new TripTransfer(fromStop, toStop, transferType, minTransferTime, fromTripId, toTripId, maxWaitingTime));
            } else {
                transfers.add(new Transfer(fromStop, toStop, transferType, minTransferTime));
            }
        }


        output.writeInt(transfers.size());
        for (Transfer transfer : transfers) {
            transfer.write(output);
        }

        output.writeInt(tripTransfers.size());
        for (TripTransfer tripTransfer : tripTransfers) {
            tripTransfer.write(output);
        }
    }

    private static class Transfer {

        private final StopInfo from;
        private final StopInfo to;
        private final int transferType;
        private final int minTransferTime;

        Transfer(StopInfo from, StopInfo to, int transferType, int minTransferTime) {
            this.from = from;
            this.to = to;
            this.transferType = transferType;
            this.minTransferTime = minTransferTime;
        }

        void write(CheckedOutputStream os) throws IOException {
            from.write(os);
            to.write(os);
            os.writeByte(transferType);
            os.writeShort(minTransferTime);
        }
    }


    private static class TripTransfer extends Transfer {

        private final int fromTrip;
        private final int toTrip;
        private final int maxWaitingTime;

        TripTransfer(StopInfo from, StopInfo to, int transferType, int minTransferTime, int fromTrip, int toTrip, int maxWaitingTime) {
            super(from, to, transferType, minTransferTime);
            this.fromTrip = fromTrip;
            this.toTrip = toTrip;
            this.maxWaitingTime = maxWaitingTime;
        }

        @Override
        void write(CheckedOutputStream os) throws IOException {
            super.write(os);

            os.writeInt(fromTrip);
            os.writeInt(toTrip);
            os.writeShort(maxWaitingTime);
        }
    }

}
