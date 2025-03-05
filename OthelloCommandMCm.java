import java.util.concurrent.atomic.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class OthelloCommandMCm{
    OthelloAgent_MCTSmateBase othello;
    BufferedReader br;

    OthelloCommandMCm(String model,
                      int thread,
                      int expand,
                      int playout,
                      double lambda,
                      float cnst,
                      boolean mate,
                      int mate_cut
    ){
        System.out.printf("model=%s thread=%d expand=%d playout=%d lambda=%f cnst=%f mate=%b mate_cut=%d\n", model, thread, expand, playout, lambda, cnst, mate, mate_cut);
        othello = (model != null
                   ? new OthelloAgent_MCTSmateNNUE(model, thread, expand, playout, lambda, cnst, mate, mate_cut)
                   : new OthelloAgent_MCTSmateBase(thread, expand, playout, cnst, mate, mate_cut)       
        );

        try{
            br = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                System.out.print("Input command: ");
                String str = br.readLine();
                String[] spl = str.split(" ");
                switch(spl[0]){
                    case "board":
                    othello.board_show(othello.st.board);                    
                    byte[][] nexts = othello.generate_moves(othello.st.board);
                    StringBuilder sb_ = new StringBuilder("available moves: ");
                    for(byte[] next: nexts){
                        sb_.append(othello.move_string(othello.st.board, next));
                        sb_.append(' ');
                    }
                    System.out.println(sb_.toString());
                    break;

                    case "position":
                    if(spl.length >= 2 && spl[1].equals("startpos"))othello.setposition(startpos(true));
                    else if(spl.length >= 2 && spl[1].equals("startpos2"))othello.setposition(startpos(false));
                    else{
                        byte[] res = new byte[65];
                        System.out.println(" 01234567");
                        for(int i=0 ; i<8 && res != null ; ++i){
                            System.out.print(i);
                            str = br.readLine();
                            int length = str.length();
                            for(int j=0 ; j<length && res != null ; ++j){
                                switch(str.charAt(j)){
                                    case 'O': res[8 * j + i] = (byte)1; break;
                                    case 'X': res[8 * j + i] = (byte)-1;
                                    case ' ': break;
                                    default: res = null;
                                }
                            }                            
                        }
                        if(res == null){
                            System.out.println("set position failed");
                            break;
                        }
                        System.out.print("Turn(b or w)");
                        str = br.readLine();
                        if(str.length() == 0){
                            System.out.println("set position failed");
                            break;
                        }
                        switch(str.charAt(0)){
                            case 'b': res[64] = (byte)1; break;
                            case 'w': res[64] = (byte)-1; break;
                            default: res = null;
                        }
                        if(res == null)System.out.println("set position failed");
                        else othello.setposition(res);
                    }
                    break;

                    case "usi":
                    StringBuilder sb = new StringBuilder("pv_interval ");
                    sb.append(othello.pv_interval);
                    sb.append("\nnodelimit ");
                    sb.append(othello.nodelimit);
                    sb.append("\nucb ");
                    sb.append(othello.ucb);
                    sb.append("\nreuse ");
                    sb.append(othello.reuse);
                    sb.append("\nc_base ");
                    sb.append(othello.c_base);
                    sb.append("\nc_init ");
                    sb.append(othello.c_init);
                    sb.append("\nvirtual_loss ");
                    sb.append(othello.virtual_loss);
                    sb.append("\ndraw_emp ");
                    sb.append(othello.draw_emp);
                    sb.append("\ninfooutput ");
                    sb.append(othello.infooutput);
                    sb.append("\nmate_all ");
                    sb.append(othello.mate_all);
                    System.out.println(sb.toString());
                    break;

                    case "setoption":
                    if(spl.length >= 3 && spl.length % 2 == 1){
                        String[] arg = new String[spl.length - 1];
                        for(int i=0 ; i<arg.length ; ++i)arg[i] = spl[i+1];
                        if(othello.setoption(arg))break;
                    }
                    System.out.println("set option failed");
                    break;

                    case "go":
                    if(!(spl.length == 1 || spl[1].equals("infinite"))){
                        boolean ok = false;
                        try{
                            float time = Float.parseFloat(spl[1]);
                            if(time >= 0)ok = true;
                        }catch(NumberFormatException e){}
                        if(!ok){
                            System.out.println("invalid time parameter");
                            break;
                        }
                    }
                    long time1 = System.currentTimeMillis();
                    if(!othello.engine_start()){
                        System.out.println("game finished");
                        break;
                    }
                    if(spl.length == 1 || spl[1].equals("infinite"))br.readLine();
                    else{
                        float time = Float.parseFloat(spl[1]);
                        while(System.currentTimeMillis() - time1 < time * 1000 && othello.searching.get());                        
                    }
                    othello.engine_stop(false);
                    othello.print_pv();
                    break;

                    case "play":
                    if(spl.length < 2){
                        System.out.println("play failed");
                        break;
                    }
                    byte[][] nexts_ = othello.generate_moves(othello.st.board);
                    for(byte[] next: nexts_){
                        if(othello.move_string(othello.st.board, next).equals(spl[1])){
                            othello.setposition(next);
                            nexts_ = null;
                            break;
                        }
                    }
                    if(nexts_ != null)System.out.println("play failed");
                    break;

                    case "go_play":                    
                    if(!(spl.length == 1 || spl[1].equals("infinite"))){
                        boolean ok = false;
                        try{
                            float time = Float.parseFloat(spl[1]);
                            if(time >= 0)ok = true;
                        }catch(NumberFormatException e){}
                        if(!ok){
                            System.out.println("invalid time parameter");
                            break;
                        }
                    }
                    long time2 = System.currentTimeMillis();
                    if(!othello.engine_start()){
                        System.out.println("game finished");
                        break;
                    }
                    if(spl.length == 1 || spl[1].equals("infinite"))br.readLine();
                    else{
                        float time = Float.parseFloat(spl[1]);
                        while(System.currentTimeMillis() - time2 < time * 1000 && othello.searching.get());                        
                    }
                    byte[] next = othello.engine_stop(true);
                    othello.print_pv();
                    System.out.println("played " + othello.move_string(othello.st.board, next));
                    othello.setposition(next);
                    break;

                    case "exit":
                    case "quit":
                    br.close();
                    System.exit(0);

                    default:
                    System.out.println("invalid command\nvalid command: board, position, usi, setoption, go, play, go_play, exit, quit");                    
                }
            }
        }catch(IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    private byte[] startpos(boolean black_ul){
        byte[] ret = new byte[65];
        ret[27] = ret[36] = (byte)(black_ul ? 1 : -1);
        ret[28] = ret[35] = (byte)-ret[27];
        ret[64] = (byte)1;
        return ret;
    }

    public static void main(String[] args){
        int thread = 0, expand = 0, playout = -1, mate_cut = 0;
        float cnst = 600;
        String model = null;
        boolean mate = false;
        double lambda = 0;
                
        String[][] param_list= {{"model", "(optional)", "model weight file(if not specified, playout only)"},
                                {"thread", "", "number of threads(positive integer)"},
                                {"expand", "", "search count threshold for expanding next nodes(positive integer)"},
                                {"playout", "(optional, default: 0(NNUE) or $expand(playout))", "number of matches for playout(0 or positive integer)"},
                                {"lambda", "(optional, default:"+lambda+")", "ratio of playout for value when NNUE(0 <= lambda < 1. Ignored when playout only)"},
                                {"cnst", "(optional, default:"+cnst+")", "temperature constant for converting value(positive float)"},
                                {"mate", "(optional, default:"+mate+")", "mate search(flag)"},
                                {"mate_cut", "(optional, default:"+mate_cut+")", "number of threads which search with mate_cut(0 or positive integer)"}
        };

        boolean thread_flg = false, expand_flg = false;
                
        int index = 0;
        boolean format_error = false;
        try{
            while(index < args.length){
                String s = args[index];
                if(s.equals(param_list[0][0])){
                    model = args[index+1];
                    index += 2;
                }else if(s.equals(param_list[1][0])){
                    thread = Integer.parseInt(args[index+1]);
                    if(thread <= 0){
                        format_error = true;
                        break;
                    }
                    thread_flg = true;
                    index += 2;
                }else if(s.equals(param_list[2][0])){
                    expand = Integer.parseInt(args[index+1]);
                    if(expand <= 0){
                        format_error = true;
                        break;
                    }
                    expand_flg = true;
                    index+=2;
                }else if(s.equals(param_list[3][0])){
                    playout = Integer.parseInt(args[index+1]);
                    if(playout < 0){
                        format_error = true;
                        break;
                    }
                    index+=2;
                }else if(s.equals(param_list[4][0])){
                    lambda = Double.parseDouble(args[index+1]);
                    if(!(lambda >= 0 && lambda < 1)){
                        format_error = true;
                        break;
                    }
                    index+=2;
                }else if(s.equals(param_list[5][0])){
                    cnst = Float.parseFloat(args[index+1]);
                    if(cnst <= 0){
                        format_error = true;
                        break;
                    }
                    index+=2;
                }else if(s.equals(param_list[6][0])){
                    mate = true;
                    ++index;
                }else if(s.equals(param_list[7][0])){
                    mate_cut = Integer.parseInt(args[index+1]);
                    if(mate_cut < 0){
                        format_error = true;
                        break;
                    }
                    index += 2;
                }
                                
                else{
                    format_error = true;
                    break;
                }
            }
        }catch(NumberFormatException e){
            format_error = true;
        }catch(ArrayIndexOutOfBoundsException e){
            format_error = true;
        }

        /*
         * String model,
                      int thread,
                      int expand,
                      int playout,
                      double lambda,
                      float cnst,
                      boolean mate,
                      int mate_cut
         */
        if(!format_error && thread_flg && expand_flg){
            new OthelloCommandMCm(model,
                                  thread,
                                  expand,
                                  (playout!=-1 ? playout : (model != null ? 0 : expand)),
                                  lambda,
                                  cnst,
                                  mate,
                                  mate_cut
            );
        }else{
            int maxlen=0;
            for(String[] info: param_list){
                int len=info[0].length()+info[1].length();
                if(len>maxlen)maxlen=len;
            }
            System.err.println("invalid parameter format");
            for(String[] info: param_list){
                System.err.println(String.format("%-"+maxlen+"s %s",info[0]+info[1],info[2]));
            }
            System.exit(1);
        }
    }        
}