package org.rtb.vexing.bidder.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.rtb.vexing.bidder.Bidder;
import org.rtb.vexing.model.openrtb.ext.response.ExtHttpCall;

import java.util.List;

/**
 * Seatid returned by a {@link Bidder}.
 * <p>
 * This is distinct from the {@link com.iab.openrtb.response.SeatBid} so that the prebid-server ext can be passed
 * back with type safety.
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class BidderSeatBid {

    /**
     * List of bids which bidder wishes to make.
     */
    List<BidderBid> bids;

    /**
     * Contains the extension for this seatbid.
     * <ul>
     * <li>If {@link #bids}.size() > 0, this will become response.seatbid[i].ext.{bidder} on the final OpenRTB
     * response.</li>
     * <li>If {@link #bids}.isEmpty() == true, this will be ignored because the OpenRTB spec doesn't allow a seatbid
     * with 0 bids.</li>
     * </ul>
     */
    ObjectNode ext;

    /**
     * List of debugging info. It should only be populated if the request.test == 1.
     * This will become response.ext.debug.httpcalls.{bidder} on the final OpenRTB response
     */
    List<ExtHttpCall> httpCalls;

    /**
     * List of errors produced by bidder. Errors should describe situations which
     * make the bid (or no-bid) "less than ideal." Common examples include:
     * <p>
     * 1. Connection issues.
     * 2. Imps with Media Types which this Bidder doesn't support.
     * 3. Timeout expired before all expected bids were returned.
     * 4. The Server sent back an unexpected Response, so some bids were ignored.
     * <p>
     * Any errors will be user-facing in the API.
     * Error messages should help publishers understand what might account for "bad" bids.
     */
    List<String> errors;
}