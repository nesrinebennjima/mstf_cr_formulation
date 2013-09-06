
/*
 * Author: Mustafa Camurli
 * Date:   17.11.2012
 * Update: 19.11.2012
 * 
 * ThesisFormulation Step 1
 * 
 **/

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import ilog.concert.*;
import ilog.concert.IloCopyManager.Check;
import ilog.cplex.*;

public class ThesisFormulation {
    
    private static int num_of_edges;

    private static int num_of_vertex;

    private static int num_of_freq;
    
    private static int num_of_timeslot = 10;
    
    private static double freq_table[][];
    
    private static boolean availability_table[][];
    
    private static McGraph g;
	
	
    private static double[][] freq_table_parser(String freq_file) throws FileNotFoundException
    {
        int count;		
        int num_of_freq;
        double[][] r_val;

        Scanner f_scan = new Scanner(new File(freq_file));

        num_of_freq = f_scan.nextInt();

        r_val = new double[num_of_freq][];

        for (int i = 0; i < num_of_freq; ++i)
        {
            r_val[i] = new double[num_of_freq];
        }

        count = 0;
        while (f_scan.hasNext())
        {
            r_val[count / num_of_freq][count % num_of_freq] = f_scan.nextInt();
            ++count;
        }

        f_scan.close();

        return (r_val);
    }

    private static boolean[][] avail_table_parser(String avail_file) throws FileNotFoundException
    {
        int temp;
        int count;		
        int num_of_edge;
        int num_of_freq;

        boolean[][] r_val;

        Scanner f_scan = new Scanner(new File(avail_file));

        num_of_edge = f_scan.nextInt();
        num_of_freq = f_scan.nextInt();

        r_val = new boolean[num_of_edge][];

        for (int i = 0; i < num_of_edge; ++i)
        {
            r_val[i] = new boolean[num_of_freq];
        }

        count = 0;
        while (f_scan.hasNext())
        {
            temp = f_scan.nextInt();
            if (temp == 0)
            {
                r_val[count / num_of_freq][ count % num_of_freq] = false;
            }
            else
            {
                r_val[count / num_of_freq][ count % num_of_freq] = true;
            }

            ++count;
        }

        f_scan.close();

        return (r_val);
    }

    private static void print_freq_table()
    {
        for (int row = 0; row < freq_table.length; ++row)
        {
            for (int col = 0; col < freq_table[row].length; ++col)
            {
                System.out.printf("%f ", freq_table[row][col]);
            }
            System.out.printf("\n");
        }
        System.out.printf("\n");
    }

    private static void print_avail_table()
    {
        for (int row = 0; row < availability_table.length; ++row)
        {
            for (int col = 0; col < availability_table[row].length; ++col)
            {
                System.out.printf("%b ", availability_table[row][col]);
            }
            System.out.printf("\n");
        }
        System.out.printf("\n");
    }
    
    public static void main(String[] args) throws FileNotFoundException 
    {
        g = new McGraph("graph.dat");

        num_of_edges = g.get_num_of_edge();
        num_of_vertex = g.get_num_of_vertex();

        freq_table = freq_table_parser("freq.dat");
        num_of_freq = freq_table.length;

        availability_table = avail_table_parser("avail.dat");

        assert(num_of_vertex == availability_table.length);
        assert(num_of_freq  == availability_table[0].length);
        
        try 
        {
            IloCplex cplex = new IloCplex();
           
            /* Decision variables */
            IloIntVar[][] et = new IloIntVar[num_of_edges][num_of_timeslot];
            IloIntVar[][] vt = new IloIntVar[num_of_vertex][num_of_timeslot];
            
            
            /* initialize variables */
            
            for (int i = 0; i < num_of_edges; ++i)
            {
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    et[i][t] = cplex.intVar(0, 1, "et["+ i + "][" + t + "]");                    
                }
            }
            
            for (int i = 0; i < num_of_vertex; ++i)
            {
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    vt[i][t] = cplex.intVar(0, 1, "vt["+ i + "][" + t + "]");
                }
            }
            
            /* construction of objective function*/
            
            IloLinearNumExpr obj = cplex.linearNumExpr();
            
            for (int i = 0; i < num_of_vertex; ++i)
            {
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    obj.addTerm(1, vt[i][t]);
                }
            }
            
            cplex.addMinimize(obj);
            
            /* constraints */
            
            /* source must be selected for a timeslot */
            {
                IloLinearIntExpr sum_of_ts = cplex.linearIntExpr(0);
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    sum_of_ts.addTerm(1, vt[0][t]);
                }
                cplex.addEq(1, sum_of_ts);
            }
            
            /* destination must be selected for a timeslot */
            {
                IloLinearIntExpr sum_of_ts = cplex.linearIntExpr(0);
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    sum_of_ts.addTerm(1, vt[num_of_vertex - 1][t]);
                }
                cplex.addEq(1, sum_of_ts);
            }
            
            /* add source and destination vertex edge constraint */
            for (int t = 0; t < num_of_timeslot; ++t)
            {
                for (int i = 0; i < num_of_edges; ++i)
                {
                    /* source */
                    if (g.has_edge(0, i))
                    {
                        cplex.addEq(vt[0][t], et[g.get_edge_index(0, i)][t]);
                    }
                    
                    /* dest */
                    if (g.has_edge(i, num_of_edges - 1))
                    {
                        cplex.addEq(vt[num_of_edges - 1][t], 
                                        et[g.get_edge_index(i, num_of_edges - 1)][t]);
                    }
                }
            }
            
            /* all internal nodes can talk with exactly one node in one timeslot */
            for (int i = 1; i < num_of_vertex - 1; ++i)
            {
                /* for a timeslot */
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    IloLinearIntExpr sum_of_edges = cplex.linearIntExpr(0);
                    /* find neighbourhood */
                    for (int j = 0; j < num_of_vertex; ++j)
                    {
                        /* in & outgoing edges */
                        if (g.has_edge(i, j))
                        {
                            sum_of_edges.addTerm(1, et[g.get_edge_index(i, j)][t]);
                            sum_of_edges.addTerm(1, et[g.get_edge_index(j, i)][t]);
                        }
                    }
                    cplex.addLe(sum_of_edges, 1);
                }
            }
            
            
            
            /* add internal vertexes edge constraints */
            
//            for (int i = 1; i < num_of_vertex - 1; ++i)
//            {
//                int factor = 1;
//                IloLinearIntExpr sum_of_edge = cplex.linearIntExpr(0);
//                IloLinearIntExpr negate_sum_of_edge = cplex.linearIntExpr(0);
//                IloLinearIntExpr forced_to_zero = cplex.linearIntExpr(0);
//                
//                IloLinearIntExpr minus_one_minus_vertex = cplex.linearIntExpr(0);
//                IloLinearIntExpr vertex_minus_one = cplex.linearIntExpr(0);
//                
//                /* -1 - v[i] */
//                minus_one_minus_vertex.addTerm(-1, cplex.intVar(1, 1));
//                minus_one_minus_vertex.addTerm(-1, vertexes[i]);
//                
//                /* v[i] - 1 */
//                vertex_minus_one.addTerm( 1, vertexes[i]);
//                vertex_minus_one.addTerm(-1, cplex.intVar(1, 1));
//                
//                /* for i.th edge */
//                for (int j = 0; j < num_of_vertex; ++j)
//                {
//                    if (g.has_edge(i, j))
//                    {
//                        sum_of_edge.addTerm(1, edges[g.get_edge_index(i, j)]);
//                        negate_sum_of_edge.addTerm(-1, edges[g.get_edge_index(i, j)]);
//                        forced_to_zero.addTerm(factor, edges[g.get_edge_index(i, j)]);
//                        
//                        factor *= -1; /* toggle between -1 and 1 */
//                    }
//                }
//                
//                /* sum_of_edge <= 2 */
//                cplex.addLe(sum_of_edge, 2);
//                
//                /* sum_of_edge can be only ( 0 or 2 )*/
//                cplex.addEq(forced_to_zero, 0);
//                
//                /* -1 - v[i] <= -sum_of_edge */
//                cplex.addLe(minus_one_minus_vertex, negate_sum_of_edge);
//                
//                /* sum_of_edge - 1 */
//                sum_of_edge.addTerm(-1, cplex.intVar(1, 1));
//                
//                /* v[i] - 1 <= sum_of_edge - 1 */
//                cplex.addLe(vertex_minus_one, sum_of_edge);
//            } /* end of internal vertex edge constraints */
            
            /* source vertex is active in 0.th time slot */
            //cplex.addEq(1, vt[0][0]); /* source must be selected */
            
            /* source and destination vertex time slot constraint */
            {
                IloLinearIntExpr s_sum_of_tslot = cplex.linearIntExpr(0);
                IloLinearIntExpr d_sum_of_tslot = cplex.linearIntExpr(0);
                
                for (int t = 0; t < num_of_timeslot; ++t)
                {
                    s_sum_of_tslot.addTerm(1, vt[0][t]);
                    d_sum_of_tslot.addTerm(1, vt[num_of_vertex - 1][t]);
                    
                }
                cplex.addEq(vertexes[0], s_sum_of_tslot);
                cplex.addEq(vertexes[num_of_vertex - 1], d_sum_of_tslot);
            }
            
            /* internal nodes timeslot allocation */
//            {
//                for (int i = 1; i < num_of_vertex - 1; ++i)
//                {
//                    IloLinearIntExpr sum_of_tslot = cplex.linearIntExpr(0);
//                    IloLinearIntExpr two_times_v  = cplex.linearIntExpr(0);
//
//                    for (int t = 0; t < num_of_timeslot; ++t)
//                    {
//                        sum_of_tslot.addTerm(1, vert_tslot[i][t]);
//                    }
//                    
//                    two_times_v.addTerm(2, vertexes[i]);
//                    cplex.addEq(two_times_v, sum_of_tslot);
//                }
//            }
            
//            for (int i = 0; i < num_of_vertex; ++i)
//            {
//                for (int j = 0; j < num_of_vertex; ++j)
//                {
//                    if (g.is_vertexes_adj(i, j) == 1)
//                    {
//                        IloLinearIntExpr k = cplex.linearIntExpr(0);
//                        
//                        for (int t = 0; t < num_of_timeslot; ++t)
//                        {
//                            k.addTerm(1, vert_tslot[i][t]);
//                            k.addTerm(1, vert_tslot[j][t]);
//                        }
//                        
//                        cplex.addEq(1, k);
//                    }
//                }
//            }
            
            
            if (cplex.solve())
            {
                cplex.output().println("Solution status = " + cplex.getStatus());
                cplex.output().println("Solution value  = " + cplex.getObjValue());
                
                for (int i = 0; i < num_of_vertex; ++i)
                {
                    cplex.output().println("vertexes[" + i + "] = " + cplex.getValue(vertexes[i]));
                }
                
                for (int i = 0; i < num_of_edges; ++i)
                {
                    cplex.output().println("edges[" + i + "] = " + cplex.getValue(edges[i]));
                }
                
                for (int i = 0; i < num_of_vertex; ++i)
                {
                    for (int t = 0; t < num_of_timeslot; ++t)
                    {
                        cplex.output().println("vert_tslot[" + i + "][" + t + "] = " + cplex.getValue(vt[i][t]));
                    }
                }
            }
            else
            {
            
                System.out.println("No solution found!");
            }
        }
        catch(IloException e)
        {
            e.printStackTrace();
        }
        
    }
}

