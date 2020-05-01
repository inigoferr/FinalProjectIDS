
public class PhysicalTopology {

    PhysicalTopology(){
    }

    public int[][] getTopology_1(){
        int[][] matrix = {{0, 0, 1, 0, 0},
                          {0, 0, 1, 1, 1},
                          {1, 1, 0, 0, 0},
                          {0, 1, 0, 0, 0},
                          {0, 1, 0, 0, 0}};
        return matrix;
    }

    public int[][] getTopology_2(){
        int[][] matrix = {{0, 0, 0, 1, 0},
                          {0, 0, 1, 0, 0},
                          {0, 1, 0, 1, 1},
                          {1, 0, 1, 0, 0},
                          {0, 0, 1, 0, 0}};
        return matrix;
    }

}