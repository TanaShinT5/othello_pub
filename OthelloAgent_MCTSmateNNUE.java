import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;

class OthelloAgent_MCTSmateNNUE extends OthelloAgent_MCTSmateBase{
    final double[][] param0=new double[128][256];
    final double[] param1=new double[param0[0].length];
    final double[][] param2=new double[param1.length][256];
    final double[] param3=new double[param2[0].length];
    final double[] param4=new double[param3.length];
    double param5;

    final double lambda;

    OthelloAgent_MCTSmateNNUE(String model,int thread,int ex,int po,double l,float c,boolean m,int mate_cut){
        super(thread, ex, po, c, m, mate_cut);

        File file=new File(model);
        if(file.exists()){            
            try{
                FileReader fr=new FileReader(file);
                BufferedReader br=new BufferedReader(fr);
                
                for(int i=0;i<param0.length;++i){
                    String[] p0=br.readLine().split(" ");
                    for(int j=param0[0].length-1;j>=0;--j){
                        param0[i][j]=Double.parseDouble(p0[j]);
                    }
                }
                String[] p1=br.readLine().split(" ");
                for(int i=param1.length-1;i>=0;--i)param1[i]=Double.parseDouble(p1[i]);
                for(int i=0;i<param2.length;++i){
                    String[] p2=br.readLine().split(" ");
                    for(int j=param2[0].length-1;j>=0;--j){
                        param2[i][j]=Double.parseDouble(p2[j]);
                    }
                }
                String[] p3=br.readLine().split(" ");
                for(int i=param3.length-1;i>=0;--i)param3[i]=Double.parseDouble(p3[i]);
                String[] p4=br.readLine().split(" ");
                for(int i=param4.length-1;i>=0;--i)param4[i]=Double.parseDouble(p4[i]);
                param5=Double.parseDouble(br.readLine());
                fr.close();
            }catch(IOException e){
                e.printStackTrace();
                System.exit(1);
            }
        }else{
            System.err.println("File not found");
            System.exit(1);
        }

        max = 100000;
        lambda = l;
        st.set_next();
    }

    @Override
    void node_value(SearchNodeMCm s){
        super.node_value(s);

        byte turn = s.board[64];
        double[] output1 = param1.clone();
        for(int i=0 ; i<64 ; ++i){
            if(s.board[i] == turn){
                double[] p = param0[i];
                for(int j=output1.length-1 ; j>=0 ; --j)output1[j] += p[j];
            }else if(s.board[i] != 0){
                double[] p = param0[64 + i];
                for(int j=output1.length-1 ; j>=0 ; --j)output1[j] += p[j];
            }
        }

        double[] output3 = param3.clone();
        for(int i=output1.length-1 ; i>=0 ; --i){
            double v = output1[i];
            if(v > 0){
                double[] p = param2[i];
                for(int j=output3.length-1 ; j>=0 ; --j)output3[j] += p[j] * v;
            }
        }

        double v = param5;
        for(int i=output3.length-1 ; i>=0 ; --i){
            double w = output3[i];
            if(w>0)v += param4[i] * w;
        }
        v = 1 / (1+Math.exp(-v));
        //System.out.printf("playout: %f(match: %d, expand: %d), nnue: %f, lambda: %f -> value: %f\n", s.value, playout, expand, v, lambda, (1-lambda) * v + lambda * s.value);
        s.value = (1-lambda) * v + lambda * s.value;
    }

    @Override
    int convert_value(double value){
        if(value==0)return -max;
        else if(value==1)return max;
        else return (int)(-cnst*Math.log(1/value -1));
    }
}