import java.util.concurrent.atomic.*;
import java.util.*;

public class OthelloLocalMCm{
    OthelloLocalMCm(int match_num,
                    int turn_change,
        
                    float time, //common settings
                    float time2,
                    boolean ponder,
                    boolean ponder2,

                    String model, //param file(null = playout)
                    String model2,                    
                    
                    int thread, //MCTS param
                    int thread2,
                    int expand,
                    int expand2,
                    int playout,
                    int playout2,
                    double lambda,
                    double lambda2,
                    float cnst,
                    float cnst2,
                    boolean mate, //mate
                    boolean mate2,
                    int mate_cut,
                    int mate_cut2,

                    int interval, //MCTS optional param
                    int interval2,
                    int ramlimit,
                    int ramlimit2,
                    boolean ucb,
                    boolean ucb2,
                    boolean reuse,
                    boolean reuse2,
                    double c_base,
                    double c_base2,
                    double c_init,
                    double c_init2,
                    int virtual_loss,
                    int virtual_loss2,
                    boolean draw_emp, //mate
                    boolean draw_emp2
                    ){
                        System.out.printf("match_num=%d turn_change=%d\n", match_num, turn_change);
                        System.out.printf("othello: time=%f ponder=%b model=%s thread=%d expand=%d playout=%d lambda=%f cnst=%f mate=%b mate_cut=%d interval=%d ramlimit=%d ucb=%b reuse=%b c_base=%f c_init=%f virtual_loss=%d draw_emp=%b\n", time, ponder, model, thread, expand, playout, lambda, cnst, mate, mate_cut, interval, ramlimit, ucb, reuse, c_base, c_init, virtual_loss, draw_emp);
                        System.out.printf("othello2: time2=%f ponder2=%b model2=%s thread2=%d expand2=%d playout2=%d lambda2=%f cnst2=%f mate2=%b mate_cut2=%d interval2=%d ramlimit2=%d ucb2=%b reuse2=%b c_base2=%f c_init2=%f virtual_loss2=%d draw_emp2=%b\n", time2, ponder2, model2, thread2, expand2, playout2, lambda2, cnst2, mate2, mate_cut2, interval2, ramlimit2, ucb2, reuse2, c_base2, c_init2, virtual_loss2, draw_emp2);
                        OthelloAgent_MCTSmateBase othello = (model != null
                                                             ? new OthelloAgent_MCTSmateNNUE(model, thread, expand, playout, lambda, cnst, mate, mate_cut)
                                                             : new OthelloAgent_MCTSmateBase(thread, expand, playout, cnst, mate, mate_cut)
                        ), othello2 = (model2 != null
                                       ? new OthelloAgent_MCTSmateNNUE(model2, thread2, expand2, playout2, lambda2, cnst2, mate2, mate_cut2)
                                       : new OthelloAgent_MCTSmateBase(thread2, expand2, playout2, cnst2, mate2, mate_cut2)
                        );
                        long nodelimit = ramlimit, nodelimit2 = ramlimit2;
                        String[] init_options = {
                            "pv_interval", Integer.toString(interval), // use if print pv
                            "nodelimit", Long.toString(nodelimit),
                            "ucb", Boolean.toString(ucb),
                            "reuse", Boolean.toString(reuse),
                            "c_base", Double.toString(c_base),
                            "c_init", Double.toString(c_init),
                            "virtual_loss", Integer.toString(virtual_loss),
                            "draw_emp", Boolean.toString(draw_emp)
                        }, init_options2 = {
                            "pv_interval", Integer.toString(interval2),
                            "nodelimit", Long.toString(nodelimit2),
                            "ucb", Boolean.toString(ucb2),
                            "reuse", Boolean.toString(reuse2),
                            "c_base", Double.toString(c_base2),
                            "c_init", Double.toString(c_init2),
                            "virtual_loss", Integer.toString(virtual_loss2),
                            "draw_emp", Boolean.toString(draw_emp2)
                        };
                        if(!othello.setoption(init_options)){
                            System.err.println("init_options set failed");
                            System.exit(1);
                        }
                        if(!othello2.setoption(init_options2)){
                            System.err.println("init_options2 set failed");
                            System.exit(1);
                        }

                        int[] results = new int[3]; //win, lose, draw for othello
                        boolean turn = (turn_change != 1);
                        Random random = new Random();
                        for(int match=0 ; match<match_num ; ++match){
                            switch(turn_change){
                                case 1:
                                turn = !turn;
                                break;

                                case 2:
                                turn = random.nextBoolean();
                            }

                            byte[] board = new byte[65];
                            board[27] = board[36] = (byte)1;
                            board[28] = board[35] = (byte)-board[27];
                            board[64] = (byte)1;

                            while(true){
                                othello.setposition(board);
                                othello2.setposition(board);
                                boolean current_turn = (turn && board[64]==1) || (!turn && board[64]==-1);

                                String[] turn_options = {
                                    "mate_all", Boolean.toString(false),
                                    "infooutput", Boolean.toString(match_num == 1)
                                }, opponent_options = {
                                    "mate_all", Boolean.toString(true),
                                    "infooutput", Boolean.toString(false)
                                };
                                if(!othello.setoption(current_turn ? turn_options : opponent_options)){
                                    System.err.println("options set failed");
                                    System.exit(1);
                                }
                                if(!othello2.setoption(!current_turn ? turn_options : opponent_options)){
                                    System.err.println("options2 set failed");
                                    System.exit(1);
                                }

                                if(!(current_turn ? othello : othello2).engine_start())break;
                                long time1 = System.currentTimeMillis();

                                if(current_turn){
                                    if(ponder2)othello2.engine_start();
                                }else{
                                    if(ponder)othello.engine_start();
                                }

                                while(System.currentTimeMillis() - time1 < (current_turn ? time : time2)*1000);

                                board = (current_turn ? othello : othello2).engine_stop(true);
                                (!current_turn ? othello : othello2).engine_stop(false);
                                //(current_turn ? othello : othello2).print_pv();                            
                            }

                            int sum_stones = 0;
                            for(int i=0; i<64; ++i)sum_stones += board[i];
                            if(!turn)sum_stones = -sum_stones;
                            if(sum_stones > 0)++results[0];
                            else if(sum_stones < 0)++results[1];
                            else ++results[2];

                            System.out.printf("Matches %d/%d (turn:%b)| win:%d lose:%d draw:%d\n", match+1, match_num, turn, results[0], results[1], results[2]);
                        }

                        SearchThreadMCm.terminate.set(true);
                    }

                    public static void main(String[] args){
                        int match_num=0, turn_change=0, thread=0, thread2=-1, expand=0, expand2=-1, playout=-1, playout2=-1, mate_cut=0, mate_cut2=-1, pv_interval=500, pv_interval2=-1, ramlimit=0, ramlimit2=-1, virtual_loss=1, virtual_loss2=-1;
                        float time=0, time2=-1, cnst=600, cnst2=-1;
                        String model=null, model2=null;
                        /*boolean*/ int ponder=1, ponder2=1, ucb=0, ucb2=0, reuse=1, reuse2=1, mate=0, mate2=0, draw_emp=0, draw_emp2=0;
                        double lambda=0, lambda2=-1, c_base=19652, c_base2=-1, c_init=1.25, c_init2=-1;
                
                        String[][] param_list= {{"match_num", "", "number of matches(positive integer)"},
                                                {"turn_change", "", "how to change turn(0=no change, 1=in turns, 2=random)"},
                                                {"model", "(optional)", "model weight file(if not specified, playout only)"},
                                                {"model2", "(optional)", "model2 weight file(if not specified, playout only)"},
                                                {"time", "(time2: optional)", "time per move(second, 0 or positive float)"},
                                                {"thread", "(thread2: optional)", "number of threads(positive integer)"},
                                                {"expand", "(expand2: optional)", "search count threshold for expanding next nodes(positive integer)"},
                                                {"playout", "(optional, default: 0(NNUE) or $expand(playout))", "number of matches for playout(0 or positive integer)"},
                                                {"lambda", "(optional, default:"+lambda+")", "ratio of playout for value when NNUE(0 <= lambda < 1. Ignored when playout only)"},
                                                {"cnst", "(optional, default:"+cnst+")", "temperature constant for converting value(positive float)"},
                                                {"pv_interval", "(optional, default:"+pv_interval+")", "PV interval(milisecond, 0 or positive integer)"},
                                                {"ramlimit", "(ramlimit2: optional)", "RAM limit(MB, positive integer)"},
                                                {"ucb", "(optional, default:"+false+")", "ucb or puct(flag)"},
                                                {"reuse", "(optional, default:"+true+")", "whether to reuse tree(!flag)"},
                                                {"c_base", "(optional, default:"+c_base+")", "c_base constant for puct(positive float)"},
                                                {"c_init", "(optional, default:"+c_init+")", "c_init constant for puct(positive float)"},
                                                {"virtual_loss", "(optional, default:"+virtual_loss+")", "virtual loss(0 or positive integer)"},
                                                {"mate", "(optional, default:"+false+")", "mate search(flag)"},
                                                {"draw_emp", "(optional, default:"+false+")", "whether emphasize when draw exists(flag)"},
                                                {"mate_cut", "(optional, default:"+mate_cut+")", "number of threads which search with mate_cut(0 or positive integer)"},
                                                {"ponder", "(optional, default:"+true+")", "whether to ponder(!flag)"}};

                        boolean match_num_flg=false, turn_change_flg=false, time_flg=false, thread_flg=false, expand_flg=false, ramlimit_flg=false;
                
                        int index = 0;
                        boolean format_error = false;
                        try{
                            while(index < args.length){
                                String s=args[index];
                                if(s.equals(param_list[0][0])){
                                    match_num = Integer.parseInt(args[index+1]);
                                    if(match_num <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    match_num_flg = true;
                                    index += 2;
                                }else if(s.equals(param_list[1][0])){
                                    turn_change = Integer.parseInt(args[index+1]);
                                    if(turn_change < 0 || turn_change > 2){
                                        format_error = true;
                                        break;
                                    }
                                    turn_change_flg = true;
                                    index += 2;
                                }
                
                                else if(s.equals(param_list[2][0])){
                                    model = args[index+1];
                                    index += 2;
                                }else if(s.equals(param_list[3][0])){
                                    model2 = args[index+1];
                                    index += 2;
                                }
                                
                                else if(s.equals(param_list[4][0])){
                                    time = Float.parseFloat(args[index+1]);
                                    if(time < 0){
                                        format_error = true;
                                        break;
                                    }
                                    time_flg = true;
                                    index += 2;
                                }else if(s.equals(param_list[4][0] + 2)){
                                    time2 = Float.parseFloat(args[index+1]);
                                    if(time2 < 0){
                                        format_error = true;
                                        break;
                                    }                                    
                                    index += 2;
                                }else if(s.equals(param_list[5][0])){
                                    thread = Integer.parseInt(args[index+1]);
                                    if(thread <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    thread_flg = true;
                                    index += 2;
                                }else if(s.equals(param_list[5][0] + 2)){
                                    thread2 = Integer.parseInt(args[index+1]);
                                    if(thread2 <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[6][0])){
                                    expand = Integer.parseInt(args[index+1]);
                                    if(expand <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    expand_flg = true;
                                    index+=2;
                                }else if(s.equals(param_list[6][0] + 2)){
                                    expand2 = Integer.parseInt(args[index+1]);
                                    if(expand2 <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }else if(s.equals(param_list[7][0])){
                                    playout = Integer.parseInt(args[index+1]);
                                    if(playout < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }else if(s.equals(param_list[7][0] + 2)){
                                    playout2 = Integer.parseInt(args[index+1]);
                                    if(playout2 < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }else if(s.equals(param_list[8][0])){
                                    lambda = Double.parseDouble(args[index+1]);
                                    if(!(lambda >= 0 && lambda < 1)){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }else if(s.equals(param_list[8][0] + 2)){
                                    lambda2 = Double.parseDouble(args[index+1]);
                                    if(!(lambda2 >= 0 && lambda2 < 1)){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }else if(s.equals(param_list[9][0])){
                                    cnst = Float.parseFloat(args[index+1]);
                                    if(cnst <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }else if(s.equals(param_list[9][0] + 2)){
                                    cnst2 = Float.parseFloat(args[index+1]);
                                    if(cnst2 <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index+=2;
                                }
                
                                else if(s.equals(param_list[10][0])){
                                    pv_interval = Integer.parseInt(args[index+1]);
                                    if(pv_interval < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[10][0] + 2)){
                                    pv_interval2 = Integer.parseInt(args[index+1]);
                                    if(pv_interval2 < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[11][0])){
                                    ramlimit = Integer.parseInt(args[index+1]);
                                    if(ramlimit <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    ramlimit_flg = true;
                                    index += 2;
                                }else if(s.equals(param_list[11][0] + 2)){
                                    ramlimit2 = Integer.parseInt(args[index+1]);
                                    if(ramlimit2 <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[12][0])){
                                    ucb = 1;
                                    ++index;
                                }else if(s.equals(param_list[12][0] + 2)){
                                    ucb2 = 1;
                                    ++index;
                                }else if(s.equals(param_list[13][0])){
                                    reuse = 0;
                                    ++index;
                                }else if(s.equals(param_list[13][0] + 2)){
                                    reuse2 = 0;
                                    ++index;
                                }else if(s.equals(param_list[14][0])){
                                    c_base = Double.parseDouble(args[index+1]);
                                    if(c_base <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[14][0] + 2)){
                                    c_base2 = Double.parseDouble(args[index+1]);
                                    if(c_base2 <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[15][0])){
                                    c_init = Double.parseDouble(args[index+1]);
                                    if(c_init <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[15][0] + 2)){
                                    c_init2 = Double.parseDouble(args[index+1]);
                                    if(c_init2 <= 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[16][0])){
                                    virtual_loss = Integer.parseInt(args[index+1]);
                                    if(virtual_loss < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[16][0] + 2)){
                                    virtual_loss2 = Integer.parseInt(args[index+1]);
                                    if(virtual_loss2 < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }
                
                                else if(s.equals(param_list[17][0])){
                                    mate = 1;
                                    ++index;
                                }else if(s.equals(param_list[17][0] + 2)){
                                    mate2 = 1;
                                    ++index;
                                }else if(s.equals(param_list[18][0])){
                                    draw_emp = 1;
                                    ++index;
                                }else if(s.equals(param_list[18][0] + 2)){
                                    draw_emp2 = 1;
                                    ++index;
                                }else if(s.equals(param_list[19][0])){
                                    mate_cut = Integer.parseInt(args[index+1]);
                                    if(mate_cut < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }else if(s.equals(param_list[19][0] + 2)){
                                    mate_cut2 = Integer.parseInt(args[index+1]);
                                    if(mate_cut2 < 0){
                                        format_error = true;
                                        break;
                                    }
                                    index += 2;
                                }

                                else if(s.equals(param_list[20][0])){
                                    ponder = 0;
                                    ++index;
                                }else if(s.equals(param_list[20][0] + 2)){
                                    ponder2 = 0;
                                    ++index;
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

                        if(!format_error && match_num_flg && turn_change_flg && time_flg && thread_flg && expand_flg && ramlimit_flg){
                            expand2 = (expand2!=-1 ? expand2 : expand);
                            new OthelloLocalMCm(match_num,
                                                turn_change,
                                                time,
                                                (time2!=-1 ? time2 : time),
                                                (ponder==1 ? true : false),
                                                (ponder2==1 ? true : false),
                                                model,
                                                model2,
                                                thread,
                                                (thread2!=-1 ? thread2 : thread),
                                                expand,
                                                expand2,
                                                (playout!=-1 ? playout : (model!=null ? 0 : expand)),
                                                (playout2!=-1 ? playout2 : (model2!=null ? 0 : expand2)),
                                                lambda,
                                                (lambda2!=-1 ? lambda2 : lambda),
                                                cnst,
                                                (cnst2!=-1 ? cnst2 : cnst),
                                                (mate==1 ? true : false),
                                                (mate2==1 ? true : false),
                                                mate_cut,
                                                (mate_cut2!=-1 ? mate_cut2 : mate_cut),
                                                pv_interval,
                                                (pv_interval2!=-1 ? pv_interval2 : pv_interval),
                                                ramlimit,
                                                (ramlimit2!=-1 ? ramlimit2 : ramlimit),
                                                (ucb==1 ? true : false),
                                                (ucb2==1 ? true : false),
                                                (reuse==1 ? true : false),
                                                (reuse2==1 ? true : false),
                                                c_base,
                                                (c_base2!=-1 ? c_base2 : c_base),
                                                c_init,
                                                (c_init2!=-1 ? c_init2 : c_init),
                                                virtual_loss,
                                                (virtual_loss2!=-1 ? virtual_loss2 : virtual_loss),
                                                (draw_emp==1 ? true : false),
                                                (draw_emp2==1 ? true : false)
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