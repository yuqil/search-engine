/**
 *  Author Yuqi Liu
 */
import java.io.*;
import java.util.*;

/**
 *  The NEAR operator for all retrieval models.
 */
public class QryIopWindow extends QryIop {
    /**
     *  Near distance
     *  Near distance is set when initializing
     */
    protected int distance;
    public void setDistance (int k) {
        this.distance = k;
        System.out.println("Near distance is " + k);
        System.out.println();
    }

    /**
     *  Evaluate the query operator; the result is an internal inverted
     *  list that may be accessed via the internal iterators.
     *  @throws IOException Error accessing the Lucene index.
     */
    protected void evaluate() throws IOException {
        //  Create an empty inverted list.  If there are no query arguments,
        //  that's the final result.
        this.invertedList = new InvList(this.getField());
        if (args.size() == 0) {
            return;
        }

        //  Each pass of the loop adds 1 document to result inverted list
        //  until all of the argument inverted lists are depleted
        while (true) {
            // find a document that contains all words, if exhausted break the loop
            int allDocid = findDoc();
            if (allDocid < 0) break;

            // find all the positions in that document that matches the near requirement
            List<Integer> positions = new ArrayList<Integer>();
            findPositions(allDocid, positions);

            // document iterator all move forward in the inverted list
            for (Qry qy : args) {
                qy.docIteratorAdvancePast(allDocid);
            }

            // if there is a match add it to inverted list
            if (positions.size() != 0) {
                Collections.sort(positions);
                this.invertedList.appendPosting(allDocid, positions);
            }
        }
    }

    /**
     * findDoc: find a document that contains all words
     *
     * @return docId if found, -1 if there is no more documents
     */
    private int findDoc() {
        //  Find the minimum next document id.  If there is none, we're done
        boolean finished = false;
        int allDocid = Qry.INVALID_DOCID;
        boolean foundMatch = false;

        while (!foundMatch) {
            Qry q0 = this.args.get(0);
            if (!q0.docIteratorHasMatch(null)) {
                finished = true;
                break;
            }
            int docid_0 = q0.docIteratorGetMatch();
            foundMatch = true;

            for (int i = 1; i < args.size(); i ++) {
                //System.err.println(docid_0);
                Qry q_i = this.args.get(i);
                q_i.docIteratorAdvanceTo(docid_0);
                if (!q_i.docIteratorHasMatch(null)) {    // If any argument is exhausted
                    finished = true;
                    break;                // there are no more matches.
                }
                int docid_i = q_i.docIteratorGetMatch();
                if (docid_0 != docid_i) {
                    foundMatch = false;
                    q0.docIteratorAdvanceTo(docid_i);
                    break;
                }
            }

            if (foundMatch) {
                allDocid = docid_0;
            }
        }

        if (finished) return -1;
        else if (foundMatch) return allDocid;
        else return -1;
    }


    /**
     * find positions that match the near requirement and records in positions list
     * @param allDocid: the document that we are checking
     * @param positions: record positions the matches the NEAR requirement
     */
    private void findPositions(int allDocid, List<Integer> positions) {
        while (true) {
            int[] pointers = new int[args.size()];
            for (int i = 0; i < this.args.size(); i++) {
                QryIop qi = (QryIop) this.args.get(i);
                if (!qi.locIteratorHasMatch()) return;
                pointers[i] = qi.locIteratorGetMatch();
            }

            int min = Integer.MAX_VALUE;
            int max = -1;

            for (int i = 0; i < pointers.length; i++) {
                if (pointers[i] < min) min = pointers[i];
                if (pointers[i] > max) max = pointers[i];
            }

            if (max - min < this.distance) {
                positions.add(max);
                for (int i = 0; i < this.args.size(); i ++) {
                    QryIop qi = (QryIop) this.args.get(i);
                    qi.locIteratorAdvancePast(pointers[i]);
                }
            } else {
                for (int i = 0; i < this.args.size(); i ++) {
                    QryIop qi = (QryIop) this.args.get(i);
                    qi.locIteratorAdvancePast(min);
                }
            }
        }
    }
}
