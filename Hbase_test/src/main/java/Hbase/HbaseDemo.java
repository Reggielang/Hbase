package Hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import java.io.IOException;


/**
 * connection 通过connectionFactory获取，是重量级的实现
 * table：主要负责DML操作
 * Admin：主要负责DDL操作
 *
 * 要什么对象就准备什么对象
 *
 */
public class HbaseDemo {
    private static Connection connection;
    static {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum","hadoop102,hadoop103,hadoop104");
        try {
            connection = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
//        createNameSpace("mydb1");
//        createTable("","t1","info1","info2");
//        dropTable("","t1");
        //putData("","stu","1003","info","name","aoteman");
//        deleteData("","stu","1002","info","name");
//        getData("","stu","1001","info","name");
//        scanData("","stu","1001","1003");
        createTableWithRegion("","staff4","info");
    }
    /**
     *
     * 创建namespace
     *
     */
    public static void createNameSpace(String namespace) throws IOException {
        //基本的判空操作
        if (namespace==null|| namespace.equals("")){
            System.err.println("namespace名字不能为空");
            return;
        }
        //获取admin对象
        Admin admin = connection.getAdmin();
        NamespaceDescriptor.Builder builder = NamespaceDescriptor.create(namespace);
        NamespaceDescriptor namespaceDescriptor = builder.build();
        try {
            //调用方法
            admin.createNamespace(namespaceDescriptor);
            System.out.println(namespace+"创建成功");
        }catch (NamespaceExistException e){
            System.err.println("namespace已经存在");
        }finally {
            admin.close();
        }

    }
    /**
     * 判断表是否存在
     *
     */
    public static boolean existsTable(String namespacename,String tablename) throws IOException {
        Admin admin = connection.getAdmin();
        return admin.tableExists(TableName.valueOf(namespacename,tablename));

    }

    /**
     *创建table
     *
     */
    public static void createTable(String namespacename,String tablename,String ... cfs) throws IOException {
        if (existsTable(namespacename,tablename)){
            System.err.println(namespacename + " : "+tablename+" 表已经存在");
            return;
        }

        Admin admin = connection.getAdmin();

        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(TableName.valueOf(namespacename,tablename));

        if (cfs==null||cfs.length<1){
            System.err.println("至少指定一个列族");
            return;
        }
        //循环创建列族
        for (String cf : cfs) {

            ColumnFamilyDescriptorBuilder columnFamilyDescriptorBuilder =
                    ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(cf));
            ColumnFamilyDescriptor columnFamilyDescriptor = columnFamilyDescriptorBuilder.build();

            tableDescriptorBuilder.setColumnFamily(columnFamilyDescriptor);
        }
        //创建表
        TableDescriptor tableDescriptor = tableDescriptorBuilder.build();
        admin.createTable(tableDescriptor);
        admin.close();
    }

    /**
     *
     * 删除表
     */

    public static void dropTable(String namespace,String tablename) throws IOException {
        if (!existsTable(namespace,tablename)){
            System.err.println("表不存在");
            return;
        }
        Admin admin = connection.getAdmin();
        //先禁用表
        TableName table1 = TableName.valueOf(namespace, tablename);
        admin.disableTable(table1);
        admin.deleteTable(table1);
        System.out.println("删除成功！");
        admin.close();
    }
    /**
     * 表的操作：put操作添加或者修改数据
     */
    public static void putData(String namespace,String tablename,String rowkey,String cf,String c1,String value) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace,tablename));
        //构造put对象
        Put put = new Put(Bytes.toBytes(rowkey));
        //加入列族，列名,数据
        put.addColumn(Bytes.toBytes(cf),Bytes.toBytes(c1),Bytes.toBytes(value));

        table.put(put);

        table.close();

    }

    /**
     * 表的操作：delete数据
     */
    public static void deleteData(String namespace,String tablename,String rowkey,String cf,String c1) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace,tablename));
        //对应hbase的deletefamily 删除对应rowkey的一整条数据
        Delete delete = new Delete(Bytes.toBytes(rowkey));

        //delete.addFamily(Bytes.toBytes(cf)); 指定删除某个列族的数据

        //delete.addColumn(Bytes.toBytes(cf),Bytes.toBytes(c1));删除某一个数据

        delete.addColumns(Bytes.toBytes(cf),Bytes.toBytes(c1)); //DeleteColumn删除某一列所有的 数据

        table.delete(delete);



        table.close();

    }


    /**
     * 表的操作 get查询
     */
    public static void getData(String namespace,String tablename,String rowkey,String cf,String c1) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace,tablename));
        Get get = new Get(Bytes.toBytes(rowkey));

        //get.addFamily(Bytes.toBytes(cf)); //返回某一列族的数据
        get.addColumn(Bytes.toBytes(cf),Bytes.toBytes(c1)); //返回某列列族里的一列数据
        Result result = table.get(get);

        Cell[] cells = result.rawCells();
        for (Cell cell : cells) {
            String cellString = Bytes.toString(CellUtil.cloneRow(cell)) + " : "+
                                Bytes.toString(CellUtil.cloneFamily(cell))+ " : "+
                                Bytes.toString(CellUtil.cloneQualifier(cell))+ " : "+
                                Bytes.toString(CellUtil.cloneValue(cell));

            System.out.println(cellString);

        }
        table.close();

    }

    /**
     * 表操作 scan扫描表
     *
     */
    public static void scanData(String namespace,String tablename,String startrow,String stoprow) throws IOException {
        Table table = connection.getTable(TableName.valueOf(namespace,tablename));

        Scan scan = new Scan();

        //设置扫描范围
        scan.withStartRow(Bytes.toBytes(startrow));
        scan.withStopRow(Bytes.toBytes(stoprow));

        ResultScanner scanner = table.getScanner(scan);

        for (Result result : scanner) {
            Cell[] cells = result.rawCells();
            for (Cell cell : cells) {
                String cellString = Bytes.toString(CellUtil.cloneRow(cell)) + " : "+
                        Bytes.toString(CellUtil.cloneFamily(cell))+ " : "+
                        Bytes.toString(CellUtil.cloneQualifier(cell))+ " : "+
                        Bytes.toString(CellUtil.cloneValue(cell));

                System.out.println(cellString);
            }
            System.out.println("-----------------------");
        }
        table.close();
    }

    /**
     *
     * 创建预分区的表
     */
    public static void createTableWithRegion(String namespacename,String tablename,String ... cfs) throws IOException {
        if (existsTable(namespacename,tablename)){
            System.err.println(namespacename + " : "+tablename+" 表已经存在");
            return;
        }

        Admin admin = connection.getAdmin();

        TableDescriptorBuilder tableDescriptorBuilder =
                TableDescriptorBuilder.newBuilder(TableName.valueOf(namespacename,tablename));

        if (cfs==null||cfs.length<1){
            System.err.println("至少指定一个列族");
            return;
        }
        //循环创建列族
        for (String cf : cfs) {

            ColumnFamilyDescriptorBuilder columnFamilyDescriptorBuilder =
                    ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(cf));
            ColumnFamilyDescriptor columnFamilyDescriptor = columnFamilyDescriptorBuilder.build();

            tableDescriptorBuilder.setColumnFamily(columnFamilyDescriptor);
        }
        //创建表
        TableDescriptor tableDescriptor = tableDescriptorBuilder.build();

        byte [][] splitkeys = new byte[4][];
        splitkeys[0] = Bytes.toBytes("1000");
        splitkeys[1] = Bytes.toBytes("2000");
        splitkeys[2] = Bytes.toBytes("3000");
        splitkeys[3] = Bytes.toBytes("4000");
        admin.createTable(tableDescriptor,splitkeys);
        admin.close();
    }

}
