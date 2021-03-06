package com.intel.distml.model.lda;

import com.intel.distml.util.HashMapMatrix;
import com.intel.distml.util.KeyRange;
import com.intel.distml.util.Matrix;
import com.intel.distml.util.primitive.IntMatrix;

import java.util.Iterator;

/**
 * Created by ruixiang on 7/10/15.
 */
public class WordTopic extends IntMatrix {

    public WordTopic(int[][] _wordTopic, KeyRange rowKeys, KeyRange colKeys) {
        super(_wordTopic, rowKeys, colKeys);
    }

    public void mergeUpdate(int serverIndex, Matrix m) {
        if (!(m instanceof HashMapMatrix))
            throw new RuntimeException("the push update is not a hash map matrix");

        HashMapMatrix hmm = (HashMapMatrix) m;
        Iterator itr = ((HashMapMatrix.KeySet) hmm.getRowKeys()).iterator();
        while (itr.hasNext()) {
            Long key = (Long) itr.next();
            int[] tmp = (int[]) hmm.get(key);
            int k = (int) (key - rowKeys.firstKey);
            for (int i = 0; i < tmp.length; i++) {
                this.values[k][i] += tmp[i];
            }
        }
    }

}
