import matplotlib.pyplot as plt
import pandas as pd

# Replace 'your_file.csv' with the path to your CSV file
reactive = f'result_reactive'
proactive = f'result_proactive'
proactiveAll = f'result_proactive_all'
proactiveAllM = f'result_proactive_multicast'
proactiveU = f'result_proactive_unsync'
proactiveAllMU = f'result_proactive_multicast_unsync'
plain = f'result_plain'


# Read the CSV file using pandas
data = pd.read_csv(reactive)
data2 = pd.read_csv(proactive)
data3 = pd.read_csv(proactiveAll)
data4 = pd.read_csv(proactiveAllM)
data5 = pd.read_csv(proactiveU)
data6 = pd.read_csv(proactiveAllM)
data7 = pd.read_csv(plain)
data8 = pd.read_csv(reactive)


# Assuming the CSV has a column named "throughput"
column_name = 'ctrrxcount'

data["rx"] = data[column_name] 
data2["rx"] = data2[column_name]
data3["rx"] = data3[column_name]
data4["rx"] = data4[column_name]
data5["rx"] = data5[column_name]
data6["rx"] = data6["ctrtxcount"]
data7["rx"] = data7[column_name]
data8["rx"] = data8[column_name] + data8["ctrtxcount"]
# Extract data from the specified column
throughput_data = data["rx"]
throughput_data2= data2["rx"]
throughput_data3= data3["rx"]
throughput_data4= data4["rx"]
throughput_data5= data5["rx"]
throughput_data6= data6["rx"]
throughput_data7= data7["rx"]
throughput_data8= data8["rx"]
# Create x-axis values (assuming sequential index is used)
x_values = range(len(throughput_data))

# Create a line plot
plt.plot(x_values, throughput_data, marker='o', linestyle='-', color='b', label="Reactive requests")
plt.plot(x_values, throughput_data8, marker='o', linestyle='--', color='b', label="Reactive total messages")

plt.plot(x_values, throughput_data2, marker='^', linestyle='-', color='r', label="Wildcard Proactive")
#plt.plot(x_values, throughput_data5, marker='^', linestyle='--', color='r', label="wildcard proactive")
plt.plot(x_values, throughput_data3, marker='>', linestyle='-', color='g', label="Exact Proactive")
plt.plot(x_values, throughput_data4, marker='<', linestyle='--', color='m', label="Broadcast Exact Proactive")
plt.plot(x_values, throughput_data6, marker='<', linestyle='-', color='m', label="Broadcast Exact Proactive sent")
#plt.plot(x_values, throughput_data7, linestyle='--', color='black', label="no controller")




# Add labels and title
plt.xlabel('Number of transmitting devices (Controller excluded)')
plt.ylabel("Frames transmitted successfully")
plt.yscale("log")
label = [2,3,4,5,6,7,8,9,10,11]
plt.xticks(x_values,label)

plt.legend( fontsize="small")

# Display the plot
plt.savefig(f"16byte_ControllerThroughput.pdf")
