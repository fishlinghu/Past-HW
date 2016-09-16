import java.net.*;
import java.rmi.*;
import java.lang.Character;
import java.rmi.server.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class Meter extends UnicastRemoteObject implements MeterInterface
    {
    // The default constructor generated by javac does not
    // declare that it throws RemoteException, and this is
    // required, as this exception is thrown by the
    // constructor for UnicastRemoteObject.
    boolean registered, deal_waiting;
    double reading_value;
    String company_name, company_location;
    String meter_name, meter_location;
    String deal_wait_for_accept;
    Scanner scanner = new Scanner(System.in);
    BrokerInterface broker;
    CompanyInterface company;

    ArrayList company_name_list;

    public class server_info
        {
        String name;
        String location;
        double tariff;
        public server_info(String in_name, String in_location)
            {
            tariff = -1;
            name = in_name;
            location = in_location;
            }
        }

    public Meter() throws RemoteException
        {
        super();
        try
            {
            broker = (BrokerInterface) Naming.lookup("rmi://localhost/broker");
            // Modify the url here
            reading_value = -1;
            registered = false;
            deal_waiting = false;
            company_name = "NONE";
            company_location = "NONE";
            }
        catch(Exception e)
            {
            e.printStackTrace();
            }
        }


    public void listAllCompany() throws RemoteException
        {
        company_name_list = broker.getCompanyNameList();
        int i = company_name_list.size() - 1;
        while(i >= 0)
            {
            System.out.printf("Company name: %s\n", company_name_list.get(i));
            System.out.println("===================================================");
            --i;
            }
        return;
        }

    public boolean registerCompany(String new_company_name) throws RemoteException
        {
        String new_company_location;

        System.out.println("");
        if(broker.isThereSuchCompany(new_company_name))
            {
            // okay there is such company
            new_company_location = broker.getCompanyLocation(new_company_name);
            if(registered==true)
                {
                // First unregister with old company
                System.out.printf("Unregister with the old company: %s\n", company_name);
                try
                    {
                    company = (CompanyInterface) Naming.lookup(company_location);
                    if(company.unregisterMeter(meter_name, InetAddress.getLocalHost().getHostName()))
                        {
                        registered = false;
                        System.out.println("Unregister successfully!");
                        }
                    else
                        {
                        System.out.println("Can't unregister with company!");
                        return false;
                        }
                    }
                catch(Exception e)
                    {
                    //e.printStackTrace();
                    broker.deleteRegisteredMeter(meter_name, company_name);
                    registered = false;
                    System.out.printf("The company %s is out of connection, unregister from %s in the broker's database. \n", company_name, company_name);
                    return false;
                    }
                }
            try
                {
                // Register with the new company
                company = (CompanyInterface) Naming.lookup(new_company_location);
                if(company.registerMeter(meter_name, meter_location))
                    {
                    company_name = new_company_name;
                    company_location = new_company_location;
                    registered = true;
                    broker.updateMeterRegistrationInfo(meter_name, meter_location, company_name);
                    System.out.printf("Successfully register with %s!\n", company_name);
                    return true;
                    }
                else
                    {
                    System.out.println("Can't register with company!");
                    return false;
                    }
                }
            catch(Exception e)
                {
                //e.printStackTrace();
                System.out.printf("The company %s is out of connection, can't register with it. \n", company_name);
                return false;
                }
            }
        else
            {
            System.out.println("No such company. ");
            return false;
            }
        }

    public void unregisterCompany() throws RemoteException
        {
        if(registered==true)
            {
            try
                {
                company = (CompanyInterface) Naming.lookup(company_location);
                if(company.unregisterMeter(meter_name, InetAddress.getLocalHost().getHostName()))
                    {
                    registered = false;
                    System.out.println("");
                    System.out.println("Unregister successfully!");
                    }
                else
                    {
                    System.out.println("");
                    System.out.println("Can't unregister with company!");
                    return;
                    }
                }
            catch(Exception e)
                {
                broker.deleteRegisteredMeter(meter_name, company_name);
                System.out.printf("The company %s is out of connection, unregister from %s in the broker's database. \n", company_name, company_name);
                // e.printStackTrace();
                return;
                }
            }
        else
            {
            System.out.println("");
            System.out.println("You are not registered with any company. ");
            return;
            }
        }

    public void requestDeal() throws RemoteException
        {
        try
            {
            System.out.println("Successfully sent request to the broker for new deal. ");
            deal_wait_for_accept = broker.receiveRequestForDeal(meter_name, InetAddress.getLocalHost().getHostName(), meter_location);
            System.out.printf("Broker found a new deal: %s. \n", deal_wait_for_accept);
            deal_waiting = true;
            return;
            }
        catch(Exception e)
            {
            e.printStackTrace();
            return;
            }
        }

    public void acceptDeal() throws RemoteException
        {
        char your_answer;
        if(deal_waiting==true)
            {
            System.out.printf("Do you accept the deal: %s ? (Y/N): ", deal_wait_for_accept);
            try
                {
                your_answer = (char) System.in.read();
                }
            catch(Exception e)
                {
                e.printStackTrace();
                return;
                }
            your_answer = Character.toLowerCase(your_answer);
            if(your_answer == 'y')
                {
                // Accept the deal
                try
                    {
                    if(broker.registerMeterWithCompany(meter_name, InetAddress.getLocalHost().getHostName(), meter_location))
                        {
                        deal_waiting = false;
                        return;
                        }
                    else
                        {
                        deal_waiting = false;
                        System.out.println("Error occurs when the broker tried to register you with a new company. ");
                        return;
                        }
                    }
                catch(Exception e)
                    {
                    e.printStackTrace();
                    return;
                    }
                }
            else if(your_answer == 'n')
                {
                deal_waiting = false;
                return;
                }
            else
                {
                System.out.println("What did you enter?");
                acceptDeal();
                }
            }
        else
            {
            System.out.println("You didn't request any deal!");
            }
        }

    public void sendAlert() throws RemoteException
        {
        if(registered==true)
            {
            try
                {
                company = (CompanyInterface) Naming.lookup(company_location);
                company.receiveAlert(meter_name, InetAddress.getLocalHost().getHostName());
                System.out.printf("\nAlert is successfully sent to %s\n", company_name);
                return;
                }
            catch(Exception e)
                {
                // e.printStackTrace();
                connectionLost();
                return;
                }
            }
        else
            {
            System.out.println("You are not registered with any company. ");
            return;
            }
        }

    public void sendReading() throws RemoteException
        {
        if(registered==true)
            {
            try
                {
                company = (CompanyInterface) Naming.lookup(company_location);
                company.receiveReading(meter_name, InetAddress.getLocalHost().getHostName(), reading_value);
                System.out.printf("\nReading is successfully sent to %s\n", company_name);
                return;
                }
            catch(Exception e)
                {
                //e.printStackTrace();
                connectionLost();
                return;
                }
            }
        else
            {
            System.out.println("You are not registered with any company. ");
            return;
            }
        }

    public double sendReadingToBroker() throws RemoteException
        {
        System.out.println("");
        System.out.println("Broker requests for the history of readings. ");
        System.out.println("Sent the history of readings to the Broker. ");
        return reading_value;
        }

    public void setReading() throws RemoteException
        {
        System.out.printf("\nReading value now is %f. \n", reading_value);
        System.out.printf("Set a new value: ");
        reading_value = scanner.nextDouble();
        System.out.printf("Suceed! Reading is now %f. \n", reading_value);
        return;
        }

    public void receiveCommand(String command) throws RemoteException
        {
        System.out.printf("\n%s says %s. \n", company_name, command);
        return;
        }

    public String getServerLocation() throws RemoteException
        {
        String whoami = "unknown";
        //InetAddress whoami;
        try
            {
            whoami = InetAddress.getLocalHost().getHostName();
            return "Hello from " + whoami;
            }
        catch(Exception e)
            {
            e.printStackTrace();
            }
        return "Error!";
        }

    public void retrieveData() throws RemoteException
        {
        company_name = broker.getRegisteredCompany(meter_name);
        company_location = broker.getRegisteredCompanyLocation(meter_name);
        if(registered==true)
            {
            try
                {
                company = (CompanyInterface) Naming.lookup(company_location);
                company.meterBackOnline(meter_name);
                return;
                }
            catch(Exception e)
                {
                connectionLost();
                return;
                }
            }
        return;
        }

    public void connectionLost() throws RemoteException
        {
        char your_answer = 'n';
        String temp_company_name;
        System.out.printf("Connection lost with %s. \n", company_name);
        System.out.printf("Do you want to register with another company? (Y/N): ");
        try
            {
            your_answer = (char) System.in.read();
            }
        catch(Exception e)
            {
            e.printStackTrace();
            }
        your_answer = Character.toLowerCase(your_answer);
        if(your_answer == 'y')
            {
            System.out.println("Following are all the companies: ");
            listAllCompany();
            System.out.printf("Which company do you want to register with? Enter the name: ");
            while((temp_company_name = scanner.nextLine()).equals(""));
            if(registerCompany(temp_company_name))
                System.out.println("Register successfully!");
            else
                System.out.println("Error occurs when trying to register with a new company");
            }
        }

    }
