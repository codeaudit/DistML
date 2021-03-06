package com.intel.distml.api;

import com.intel.distml.api.DMatrix;
import com.intel.distml.api.Model;
import com.intel.distml.api.ModelWriter;
import com.intel.distml.api.databus.ServerDataBus;
import com.intel.distml.util.KeyRange;
import com.intel.distml.util.Matrix;

/**
 * Created by yunlong on 4/29/15.
 */
public class DummyModelWriter implements ModelWriter {

    private static final int MAX_FETCH_SIZE = 2048000000;   // 2g

    protected int maxFetchRows;

    public DummyModelWriter(int estimatedRowSize) {
        maxFetchRows = MAX_FETCH_SIZE / estimatedRowSize;
    }

    @Override
    public void writeModel (Model model, ServerDataBus dataBus) {

        for (String matrixName : model.dataMap.keySet()) {
            DMatrix m = model.dataMap.get (matrixName);
            long paramCount = 0L;

            if (m.hasFlag(DMatrix.FLAG_PARAM)) {
                long size = m.getRowKeys().size();
                System.out.println("total param: " + size);

                m.setLocalCache(null);
                long start = 0L;
                while(start < size-1) {
//                    if (start == 0L) {
//                        com.esotericsoftware.minlog.Log.TRACE = true;
//                    }
                    long end = Math.min(start + maxFetchRows, size) - 1;
                    KeyRange range = new KeyRange(start, end);
                    System.out.println("fetch param: " + range);
                    Matrix result = dataBus.fetchFromServer (matrixName, range);
                    paramCount += result.getRowKeys().size();
                    System.out.println("fetch done: " + paramCount);
                    //if (start == 0L) {
                    //    com.esotericsoftware.minlog.Log.TRACE = false;
                    //}
                    start = end + 1;
                }
            }
        }
    }
}
