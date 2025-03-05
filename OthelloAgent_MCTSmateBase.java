import java.util.*;
import java.util.concurrent.atomic.*;

class OthelloAgent_MCTSmateBase implements OthelloAgentBase{
    final AtomicBoolean searching=new AtomicBoolean();
    final AtomicLong nodes=new AtomicLong();
    SearchNodeMCm st=new SearchNodeMCm();
    long time1,time2;

    final SearchThreadMCm[] threads;
    final int expand, playout;
    final float cnst;

    boolean infooutput=true,reuse=true,ucb,draw_emp,mate_all;
    int pv_interval=500, max=100000, virtual_loss=1;
    double c_base=19652, c_init=1.25;
    long nodelimit=Long.MAX_VALUE;

    OthelloAgent_MCTSmateBase(int thread,int ex,int po,float c,boolean m,int mate_cut){
        threads=new SearchThreadMCm[thread];

        threads[0]=new SearchThreadMCm(this, mate_cut-- >0);
        for(int i=1;i<thread-1;++i)threads[i]=new SearchThreadMCm(this, mate_cut-- >0);
        if(thread!=1)threads[thread-1]=(m ? new MateThreadMCm(this) : new SearchThreadMCm(this, mate_cut-- >0));
        expand = ex;
        playout = po;
        cnst = c;
    }

    public boolean setoption(String[] options){
        if(searching.get())return false;
        int index=0;
        try{
            while(index < options.length){
                switch(options[index]){
                    case "infooutput":
                    case "ucb":
                    case "reuse":
                    case "draw_emp":
                    case "mate_all":
                    Boolean.parseBoolean(options[index+1]);
                    break;

                    case "virtual_loss":
                    int loss = Integer.parseInt(options[index+1]);
                    if(loss < 0)return false;
                    break;

                    case "pv_interval":
                    int interval=Integer.parseInt(options[index+1]);
                    if(interval<=0)return false;
                    break;

                    case "c_base":
                    case "c_init":
                    double c=Double.parseDouble(options[index+1]);
                    if(c<=0)return false;
                    break;

                    case "nodelimit":
                    long nodel=Long.parseLong(options[index+1]);
                    if(nodel<=0)return false;
                    break;

                    default:
                    return false;
                }
                index+=2;
            }
        }catch(NumberFormatException e){
            return false;
        }catch(ArrayIndexOutOfBoundsException e){
            return false;
        }

        index=0;
        while(index < options.length){
            switch(options[index]){
                case "infooutput":
                infooutput=Boolean.parseBoolean(options[index+1]);
                break;

                case "ucb":
                ucb=Boolean.parseBoolean(options[index+1]);
                break;

                case "reuse":
                reuse=Boolean.parseBoolean(options[index+1]);
                break;

                case "pv_interval":
                pv_interval=Integer.parseInt(options[index+1]);
                break;

                case "c_base":
                c_base=Double.parseDouble(options[index+1]);
                break;

                case "c_init":
                c_init=Double.parseDouble(options[index+1]);
                break;

                case "virtual_loss":
                virtual_loss = Integer.parseInt(options[index+1]);
                break;

                case "nodelimit":
                nodelimit=Long.parseLong(options[index+1]);
                break;

                case "draw_emp":
                draw_emp=Boolean.parseBoolean(options[index+1]);
                break;

                case "mate_all":
                mate_all=Boolean.parseBoolean(options[index+1]);
                break;
            }
            index+=2;
        }
        return true;
    }

    public boolean setposition(byte[] b){
        if(searching.get())return false;
        if(reuse){
            st=new SearchNodeMCm(b,null);
            st.set_next();
            return true;
        }
        if(Arrays.equals(st.board, b)){
            st.previous=null;
            st.set_next();
            return true;
        }
        if(st.next!=null){
            for(SearchNodeMCm s: st.next){
                if(Arrays.equals(s.board,b)){
                    st=s;
                    st.previous=null;
                    st.set_next();
                    return true;
                }
            }
        }
        if(st.previous!=null && Arrays.equals(st.previous.board,b)){
            st=st.previous;
            st.previous=null;
        }else{
            st=new SearchNodeMCm(b,null);
        }
        st.set_next();
        return true;
    }
    void node_value(SearchNodeMCm s){
        if(playout == 0){
            s.value = 0.5;
            return;
        }
        float value = 0;
        Random random = new Random();
        for(int i=playout-1 ; i>=0 ; --i){
            byte[] tmp = s.board;
            while(true){
                byte[][] next = generate_moves(tmp);
                int length = next.length;
                if(length == 0){
                    int count = 0;
                    for(int j=0 ; j<64 ; ++j)count += tmp[j];
                    if(count == 0)value += 0.5f;
                    else if(count*s.board[64] > 0)value += 1;
                    break;
                }
                tmp = next[random.nextInt(length)];
            }
        }
        s.value = value / playout;
    }
    public byte[] get_next(int index){
        return st.next[index].board;
    }

    public boolean engine_start(){
        time1=time2=System.currentTimeMillis();
        if(st.next.length==0)return false;
        nodes.set(0);
        searching.set(true);
        return true;
    }
    public byte[] engine_stop(boolean result){
        searching.set(false);
        for(SearchThreadMCm t: threads){
            t.finish.set(true);
            while(t.finish.get());
        }
        /*for(int c: st.counts)System.out.print(c+" ");
        System.out.println();
        for(int i=st.next.length-1;i>=0;--i)System.out.print((st.values[i]/st.counts[i])+" ");
        System.out.println();*/
        if(!result)return null;

        return st.next[st.get_index()].board;
    }

    synchronized void st_update(){
        long time=System.currentTimeMillis();
        StringBuilder sb_ = new StringBuilder("values info\n");
        /*for(long v: st.counts){
            sb_.append(' ');
            sb_.append(v);
        }
        sb_.append('\n');
        for(double v: st.values){
            sb_.append(' ');
            sb_.append(String.format("%4f", v));
        }
        sb_.append('\n');
        for(int j=0 ; j<st.counts.length ; ++j){
            sb_.append(' ');
            sb_.append(String.format("%4f", st.values[j] / st.counts[j]));
        }
        System.out.println(sb_.toString());*/
        if(!infooutput || time-time2<pv_interval)return;

        print_pv();
        time2=time;
    }
    public void print_pv(){
        int[] index_value=st.get_index_value();
        int depth=1;
        StringBuilder pv=new StringBuilder(move_string(st.board,st.next[index_value[0]].board));
        SearchNodeMCm s=st.next[index_value[0]];
        while(true){
            int index=s.get_index();
            if(index==-1)break;
            ++depth;
            pv.append(' ');
            pv.append(move_string(s.board,s.next[index].board));
            s=s.next[index];
        }
        StringBuilder sb=new StringBuilder("Time=");
        sb.append(String.format("%.3f,Depth=",(System.currentTimeMillis()-time1)/1000f));
        sb.append(depth);
        sb.append(",Node=");
        sb.append(nodes.get());
        sb.append(": (");
        sb.append(index_value[1]);
        sb.append(')');
        sb.append(pv.toString());
        System.out.println(sb.toString());

    }
    int convert_value(double value){
        if(value==0)return -max;
        else if(value==1)return max;
        else return (int)(-cnst*Math.log(1/value -1));
    }


    class SearchNodeMCm{
        final byte[] board;
        SearchNodeMCm previous;
        SearchNodeMCm[] next;
        double value=-1;
        double[] values;
        int[] counts;
        byte[] mates;
        long node_count=0; //fixed=0;
        SearchNodeMCm(){
            board=new byte[65];
            board[27]=board[36]=(byte)1;
            board[28]=board[35]=(byte)-board[27];
            board[64]=(byte)1;
        }
        SearchNodeMCm(byte[] b,SearchNodeMCm p){
            board=b;previous=p;
        }
    
        synchronized void set_next(){
            if(next==null){
                byte[][] nexts=generate_moves(board);
                nodes.addAndGet(nexts.length);
                next=new SearchNodeMCm[nexts.length];
                for(int i=nexts.length-1;i>=0;--i)next[i]=new SearchNodeMCm(nexts[i],this);
                values=new double[nexts.length];
                counts=new int[nexts.length];
                mates=new byte[nexts.length]; // 3=win exists, 2=draw or win exists, 1=draw, 0=unknown, -1=lose exists
                if(nexts.length==0){
                    int count=0;
                    for(int i=0;i<64;++i)count+=board[i];
                    value=(count==0 ? 0.5 : count*board[64]>0 ? 1 : 0);
                }else{
                    node_value(this);
                    count_update(nexts.length);
                }
            }        
        }
        void count_update(int num){
            synchronized(this){
                node_count += num;
                if(this==st && node_count>=nodelimit){
                    searching.set(false);
                    return;
                }
            }
            if(previous!=null)previous.count_update(num);
        }
        synchronized boolean next_is_null(){
            return next==null;
        }
    
        synchronized int get_task(boolean mate_cut){ // consider N becomes larger than Integer.MAX_VALUE
            set_next();
            if(next.length==0)return -1;
    
            if(mate_cut){
                boolean unfinished=false;
                for(byte m: mates){
                    if(m==2 || m==0){
                        unfinished=true;
                        break;
                    }
                }
                if(!unfinished)mate_cut=false;
            }
            if(ucb){
                int N=0;
                for(int i=next.length-1;i>=0;--i){
                    int c=counts[i];
                    if(c==0){
                        counts[i] += virtual_loss;
                        return i;
                    }
                    N+=c;
                }
                double c=2*Math.log(N);

                int index=-1;
                double maxval=-1;
                for(int i=next.length-1;i>=0;--i){
                    byte mate=mates[i];
                    if(mate_cut && (mate==3 || mate==1 || mate==-1))continue;

                    int count=counts[i];
                    double ucb=values[i]/count + Math.sqrt(c/count);
                    if(ucb>maxval){
                        index=i;
                        maxval=ucb;
                    }
                }

                counts[index] += virtual_loss;
                return (counts[index] < expand ? index : next.length+index);
            }else{
                int N=0;
                for(int count: counts)N+=count;
                double c=(c_init + Math.log((1+N+c_base)/c_base))*Math.sqrt(N)/next.length;
    
                int index=-1;
                double maxval=-1;
                for(int i=next.length-1;i>=0;--i){
                    byte mate=mates[i];
                    if(mate_cut && (mate==3 || mate==1 || mate==-1))continue;

                    int count=counts[i];
                    double alphazero=(count!=0 ? values[i]/count : 0) + c/(1+count);
                    if(alphazero > maxval){
                        index=i;
                        maxval=alphazero;
                    }
                }
    
                counts[index] += virtual_loss;
                return (counts[index] < expand ? index : next.length+index);
            }
        }
    
        synchronized void backup(int index,double val){
            values[index] += val;
            counts[index] -= virtual_loss - 1;
            /*if(this==st){
                ++st_count[index][val<1./3 ? 0 : val<2./3 ? 1 : 2];    
                if(st_check.get()){
                    st_check.set(false);
                    StringBuilder sb=new StringBuilder();
                    for(int i=0;i<next.length;++i){
                        sb.append(counts[i]);
                        sb.append(' ');
                        sb.append(values[i]);
                        sb.append(' ');
                        sb.append(values[i]/counts[i]);
                        sb.append(" [");
                        sb.append(st_count[i][0]);
                        sb.append(' ');
                        sb.append(st_count[i][1]);
                        sb.append(' ');
                        sb.append(st_count[i][2]);
                        sb.append(']');
                        sb.append('\n');
                    }
                    System.out.print(sb.toString());
                    st_count=new int[next.length][3];
                }
            }*/
        }
        
        synchronized int[] get_index_value(){
            byte max_mate=-1;
            for(byte v: mates){
                max_mate=(v>max_mate ? v : max_mate);
            }

            int index=-1;
            if(!draw_emp && max_mate==1){
                for(int i=next.length-1;i>=0;--i){
                    if(mates[i] == -1)continue;
                    if(index==-1 || counts[i]>counts[index] ||(counts[i]==counts[index] && values[i]>values[index]))index=i;
                }
                double v = values[index]/counts[index];
                if(mates[index]==0 && v > 0.5){
                    int[] ret={index,board[64]*convert_value(v)};
                    return ret;
                }
                index=-1;
            }
            for(int i=next.length-1;i>=0;--i){
                if(mates[i] != max_mate)continue;
                if(index==-1 || counts[i]>counts[index] ||(counts[i]==counts[index] && values[i]>values[index]))index=i;
            }

            int[] ret={index,board[64]*convert_value(max_mate==3 ? 1 : max_mate==1 ? 0.5 : max_mate==-1 ? 0 : values[index]/counts[index])};
            return ret;
        }
        synchronized int get_index(){
            if(next==null || next.length==0)return -1;
            byte max_mate=-1;
            for(byte v: mates){
                max_mate=(v>max_mate ? v : max_mate);
            }

            int index=-1;
            if(!draw_emp && max_mate==1){
                for(int i=next.length-1;i>=0;--i){
                    if(mates[i] == -1)continue;
                    if(index==-1 || counts[i]>counts[index] ||(counts[i]==counts[index] && values[i]>values[index]))index=i;
                }
                if(values[index]/counts[index] > 0.5)return index;
                index=-1;
            }
            for(int i=next.length-1;i>=0;--i){
                if(mates[i] != max_mate)continue;
                if(index==-1 || counts[i]>counts[index] || (counts[i]==counts[index] && values[i]>values[index]))index=i;
            }
            return index;
        }

        synchronized byte[][] get_mate(){
            if(counts==null){
                byte[] array=new byte[next.length];
                for(int i=array.length-1;i>=0;--i)array[i]=(byte)i;
                byte[][] result={array, mates.clone()};
                return result;
            }
            int[][] lists=new int[5][next.length];
            for(int[] l: lists)Arrays.fill(l,-1);
            for(int i=next.length-1;i>=0;--i){
                int[] l=lists[3-mates[i]];
                for(int j=0;true;++j){
                    int tmp=l[j];
                    if(tmp==-1 || counts[i]>counts[tmp] || (counts[i]==counts[tmp] && values[i]>values[tmp])){
                        tmp=i;
                        for(int k=j;true;++k){
                            int tmp2=l[k];
                            l[k]=tmp;
                            if(tmp2==-1)break;
                            tmp=tmp2;
                        }
                        break;
                    }
                }
            }
            byte[] array=new byte[next.length];
            int index=0;
            for(int[] l: lists){
                for(int v: l){
                    if(v==-1)break;
                    array[index++]=(byte)v;
                }
            }
            byte[][] result={array,mates.clone()};
            return result;
        }
        synchronized byte set_mate(int index, byte val){
            switch(mates[index]){
                case 3, 1, -1:
                return mates[index];

                case 2:
                if(val == 3 || val == 1)mates[index] = val;
                return mates[index];

                default:
                mates[index] = val;
                return val;
            }
        }
    }
}


class SearchThreadMCm extends Thread{
    OthelloAgent_MCTSmateBase othello;
    final AtomicBoolean finish = new AtomicBoolean();
    static AtomicBoolean terminate = new AtomicBoolean();
    boolean mate_cut;
    SearchThreadMCm(OthelloAgent_MCTSmateBase o, boolean mc){
        othello = o; mate_cut = mc;
        this.start();
    }

    public void run(){
        while(!terminate.get()){
            if(othello.searching.get()){
                thread_search();
            }else if(finish.get())finish.set(false);
        }
    }

    void thread_search(){
        OthelloAgent_MCTSmateBase.SearchNodeMCm s=othello.st;
        int index=-1;
        while(true){
            index=s.get_task(mate_cut);
            if(index < s.next.length)break;
            s=s.next[index-s.next.length];
            index=-1;
        }

        if(index!=-1){
            s.next[index].set_next();
        }else{
            for(int i=s.previous.next.length-1;i>=0;--i){
                if(s.previous.next[i]==s){
                    index=i;
                    s=s.previous;
                    break;
                }
            }
        }
        double set_val=s.next[index].value;
        while(true){
            set_val=(s.board[64]==s.next[index].board[64] ? set_val : 1-set_val);
            s.backup(index,set_val);
            if(s.previous==null)break;
            index=-1;
            for(int i=s.previous.next.length-1;i>=0;--i){
                if(s.previous.next[i]==s){
                    index=i;
                    s=s.previous;
                    break;
                }
            }
        }
        
        othello.st_update();
    }
}


class MateThreadMCm extends SearchThreadMCm{
    MateThreadMCm(OthelloAgent_MCTSmateBase o){
        super(o, false);
    }

    @Override
    void thread_search(){
        byte result=mate_search(othello.st, 60, othello.mate_all, false);
        if(othello.infooutput && result != 0)System.out.println(result);
        if(!othello.mate_all && result==3){
            othello.searching.set(false);
        }else{
            result=mate_search(othello.st, 60, true, true);
            //if(othello.infooutput && result!=0)System.out.println("[]" + result);
            if(!othello.mate_all && (result==3 || result==1 || result==-1))othello.searching.set(false);
        }
        /*for(int depth=1 ; !terminate.get() && othello.searching.get() ; ++depth){
            byte result = mate_search(othello.st, depth, othello.mate_all, false);
            if(!othello.mate_all && result == 3){
                if(othello.infooutput)System.out.println(depth + ": " + result);
                othello.searching.set(false);
                break;
            }else if(result != 0 && result != 2){
                if(othello.infooutput)System.out.println(depth + ":: " + result);
                result = mate_search(othello.st, 60, true, true);
                if(!othello.mate_all && (result == 3 || result == 1 || result == -1))othello.searching.set(false);
                if(othello.infooutput)System.out.println(depth + "::: " + result);
                break;
            }
        }*/
    }

    private byte mate_search(OthelloAgent_MCTSmateBase.SearchNodeMCm s,
                             int depth,
                             boolean all,
                             boolean full){
        if(s.next_is_null()){
            s=othello.new SearchNodeMCm(s.board,s.previous);
            byte[][] nexts=othello.generate_moves(s.board);
            s.next=new OthelloAgent_MCTSmateBase.SearchNodeMCm[nexts.length];
            for(int i=nexts.length-1;i>=0;--i)s.next[i]=othello.new SearchNodeMCm(nexts[i],s);
            s.mates=new byte[nexts.length];
            if(nexts.length==0){
                int count=0;
                for(int i=0;i<64;++i)count+=s.board[i];
                s.value=(count==0 ? 0.5 : count*s.board[64]>0 ? 1 : 0);
            }
        }
        if(s.next.length==0){
            return (byte)(s.value==1 ? 3 : s.value==0 ? -1 : 1);
        }

        if(!othello.searching.get() || depth == 0)return 0;

        // 3: exists 3(confirmed)
        // 2: exists 2, or (exists 1 and exists other 0)
        // 1: exists 1 and all others (1 or -1) (confirmed)
        // 0: exists 0
        // -1: all -1(confirmed)
        int result=-1;
        byte[][] mates=s.get_mate();

        for(byte i: mates[0]){
            byte tmp=mates[1][i];
            switch(tmp){
                case 3:
                result=3;
                if(!all)return 3;
                continue;

                case 1:
                if(result==-1)result=1;
                else if(result==0)result=2;

                case -1:
                continue;
            }

            OthelloAgent_MCTSmateBase.SearchNodeMCm n=s.next[i];
            tmp=mate_search(n, depth-1, full, full);
            if(s.board[64]!=n.board[64]){
                switch(tmp){
                    case 3:
                    tmp=-1;
                    break;

                    case 2:
                    tmp=0;
                    break;

                    case 1:
                    case 0:
                    break;

                    case -1:
                    tmp=3;
                }
            }
            tmp = s.set_mate(i, tmp);
            switch(tmp){
                case 3:
                if(!all)return 3;
                result=3;
                break;

                case 2:
                if(result!=3)result=2;
                break;

                case 1:
                if(result==-1)result=1;
                else if(result==0)result=2;
                break;

                case 0:
                if(result==-1)result=0;
                else if(result==1)result=2;
                break;
            }
        }
        return (byte)result;
    }
}