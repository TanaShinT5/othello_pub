import java.util.*;

interface OthelloAgentBase{
    private byte[][] itte(byte[] b){
        byte[][] res=new byte[52][];
        int count=0;
        for(int p=0;p<64;++p){
            if(b[p]==0){
                byte[] b1=b.clone();
                b1[64]=(byte)-b1[64];
                b1[p]=b[64];
                boolean puttable=false;
                int i=p/8,j=p%8;
                if(pieceTurn(b1,i,j,-1,-1))puttable=true;
                if(pieceTurn(b1,i,j,-1,0))puttable=true;
                if(pieceTurn(b1,i,j,-1,1))puttable=true;
                if(pieceTurn(b1,i,j,0,-1))puttable=true;
                if(pieceTurn(b1,i,j,0,1))puttable=true;
                if(pieceTurn(b1,i,j,1,-1))puttable=true;
                if(pieceTurn(b1,i,j,1,0))puttable=true;
                if(pieceTurn(b1,i,j,1,1))puttable=true;
                if(puttable)res[count++]=b1;
            }
        }
        return Arrays.copyOfRange(res,0,count);
    }
    private boolean pieceTurn(byte[] b,int x,int y,int dx,int dy){
        int i=x+dx,j=y+dy;
        byte init=b[8*x+y];
        if(i>=0&&i<8&&j>=0&&j<8&&b[8*i+j]==-init){
            i+=dx;j+=dy;
            while(i>=0&&i<8&&j>=0&&j<8){
                byte tmp=b[8*i+j];
                if(tmp==0)return false;
                else if(tmp==init){
                    i-=dx;j-=dy;
                    while(!(i==x && j==y)){
                        b[8*i+j]=init;
                        i-=dx;j-=dy;
                    }
                    return true;
                }
                i+=dx;j+=dy;
            }
        }
        return false;
    }
    default int movecount(byte[] b){
        int result=0;
        for(int p=0;p<64;++p){
            if(b[p]==0){
                int i=p/8,j=p%8;
                if(pieceTurnable(b,i,j,-1,-1)||pieceTurnable(b,i,j,-1,0)||pieceTurnable(b,i,j,-1,1)||pieceTurnable(b,i,j,0,-1)||pieceTurnable(b,i,j,0,1)||pieceTurnable(b,i,j,1,-1)||pieceTurnable(b,i,j,1,0)||pieceTurnable(b,i,j,1,1))++result;
            }
        }
        return result;
    }
    private boolean moveable(byte[] b){
        for(int p=0;p<64;++p){
            if(b[p]==0){
                int i=p/8,j=p%8;
                if(pieceTurnable(b,i,j,-1,-1)||pieceTurnable(b,i,j,-1,0)||pieceTurnable(b,i,j,-1,1)||pieceTurnable(b,i,j,0,-1)||pieceTurnable(b,i,j,0,1)||pieceTurnable(b,i,j,1,-1)||pieceTurnable(b,i,j,1,0)||pieceTurnable(b,i,j,1,1))return true;
            }
        }
        return false;
    }
    private boolean pieceTurnable(byte[] b,int x,int y,int dx,int dy){
        int i=x+dx,j=y+dy;
        byte init=b[64];
        if(i>=0&&i<8&&j>=0&&j<8&&b[8*i+j]==-init){
            i+=dx;j+=dy;
            while(i>=0&&i<8&&j>=0&&j<8){
                byte tmp=b[8*i+j];
                if(tmp==0)return false;
                else if(tmp==init)return true;
                i+=dx;j+=dy;
            }
        }
        return false;
    }
    default byte[][] generate_moves(byte[] b){
        byte[][] res=itte(b);
        for(byte[] r:res){
            if(!moveable(r))r[64]=(byte)-r[64];
        }
        return res;
    }
    
    default String move_string(byte[] b,byte[] a){
        for(int i=0;i<64;++i){
            if(b[i]==0&&a[i]!=0){
                StringBuilder sb=new StringBuilder();
                sb.append(i/8);
                sb.append(i%8);
                sb.append(a[i]==1 ? 'B' : 'W');
                return sb.toString();
            }
        }
        return null;
    }
    default void board_show(byte[] b){
        StringBuilder sb = new StringBuilder("01234567\n");
        for(int j=0 ; j<8 ; ++j){
            for(int i=0 ; i<8 ; ++i){
                switch(b[8 * i + j]){
                    case 1: sb.append('O'); break;
                    case -1: sb.append('X'); break;
                    default: sb.append(' ');
                }
            }
            sb.append(j);
            sb.append('\n');
        }
        sb.append((b[64] == 1 ? "Turn:Black" : "Turn:White"));
        System.out.println(sb.toString());
    }

    public boolean setoption(String[] options);
    public boolean setposition(byte[] b);

    public boolean engine_start();
    public byte[] engine_stop(boolean result);
}