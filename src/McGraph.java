

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class McGraph {

    private int[][] graph_table;
    private int[][] edge_id_table;
    private int     num_of_edges;
    private int     num_of_vertex;

    public McGraph(String graph_file) throws FileNotFoundException
    {
        int count;
        Scanner f_scan = new Scanner(new File(graph_file));

        num_of_vertex = f_scan.nextInt();

        graph_table   = new int[num_of_vertex][num_of_vertex];
        edge_id_table = new int[num_of_vertex][num_of_vertex];

        count = 0;
        while (f_scan.hasNext()) 
        {
            graph_table[count / num_of_vertex][count % num_of_vertex] = f_scan.nextInt();
            ++count;
        }
    
        /* check symmetry */
        for (int i = 0; i < num_of_vertex; ++i)
        {
            for (int j = 0; j < num_of_vertex; ++j)
            {
                assert graph_table[i][j] == graph_table[j][i];
            }
        }
        
        /* initialize  edge table */
        for (int i = 0; i < num_of_vertex; ++i)
        {
            for (int j = 0; j < num_of_vertex; ++j)
            {
                edge_id_table[i][j] = -1;		
            }
        }

        num_of_edges = 0;
        for (int i = 0; i < num_of_vertex; ++i)
        {
            for (int j = i + 1; j < num_of_vertex; ++j)
            {
                if (graph_table[i][j] != 0)
                {
                    edge_id_table[i][j] = num_of_edges;
                    edge_id_table[j][i] = num_of_edges;
                    ++num_of_edges;
                }
            }
        }
        f_scan.close();
    }

	public boolean has_edge(int s, int d)
	{
		if (!((s < num_of_vertex) && (d < num_of_vertex)))
		{
			return (false);
		}
		
		if (graph_table[s][d] > 0)
		{
			return (true);
		}
		
		return (false);
	}
	
	public int get_edge_index(int s, int d)
	{
		if (!((s < num_of_vertex) && (d < num_of_vertex)))
		{
			return (-1);
		}
		
		if (graph_table[s][d] > 0)
		{
			return (edge_id_table[s][d]);
		}
		
		return (-1);
	}
	
	public int get_num_of_vertex()
	{
		return (num_of_vertex);
	}
	
	public int get_num_of_edge()
	{
		return (num_of_edges);
	}
	
	private void print_table(int table[][])
	{
	    for (int i = 0; i < table.length; ++i)
        {
            for (int j = 0; j < table[i].length; ++j)
            {
                System.out.printf("%d ", table[i][j]);
            }
            System.out.printf("\n"); 
        }
        System.out.printf("\n");
	}
	
	public void print_graph_table()
	{
	    print_table(graph_table);
	}
	
	public void print_edge_table()
	{
	    print_table(edge_id_table);
	}
	
	public int is_vertexes_adj(int v1, int v2)
	{
	    return (graph_table[v1][v2]);
	}
	
	public int is_adjacent(int e1, int e2)
	{
		int s1 = -1;
		int d1 = -1;
		int s2 = -1;
		int d2 = -1;
		int index = 0;
		
		if (!((e1 < num_of_edges) && (e2 < num_of_edges)))
		{
			return (0);
		}
		
		for (int i = 0; i < num_of_vertex; ++i)
		{
			for (int j = 0; j < num_of_vertex; ++j)
			{
				if (index == e1)
				{
					s1 = i;
					d1 = j;
				}
				
				if (index == e2)
				{
					s2 = i;
					d2 = j;
				}
				
				if (graph_table[i][j] > 0)
				{
					++index;
				}
			}
		}
		
		assert((s1 != -1) && (d1 != -1) && (s2 != -1) && (d2 != -1));
		
		if ((s1 == d2) || (d1 == s2))
		{
			return (1);
		}
		
		return (0);
	}
}

