import matplotlib.pyplot as plt
import pandas as pd

# Replace 'your_file.csv' with the path to your CSV file
csv_file_path = 'result_reactive'
proactive = 'result_proactive'
proactiveAll = 'result_proactive_all'

# Read the CSV file using pandas
data = pd.read_csv(csv_file_path)
data2 = pd.read_csv(proactive)
data3 = pd.read_csv(proactiveAll)

# Assuming the CSV has a column named "throughput"
column_name = 'througput'


# Extract data from the specified column
throughput_data = data[column_name]
throughput_data2= data2[column_name]
throughput_data3= data3[column_name]
# Create x-axis values (assuming sequential index is used)
x_values = range(len(throughput_data))

# Create a line plot
plt.plot(x_values, throughput_data, marker='o', linestyle='-', color='b', label="reactive")
plt.plot(x_values, throughput_data2, marker='^', linestyle='-', color='r', label="wildcard proactive")
plt.plot(x_values, throughput_data3, marker='>', linestyle='-', color='g', label="exact proactive")



# Add labels and title
plt.xlabel('Number of transmitting devices (Controller excluded)')
plt.ylabel("Normalized throughput")
label = [2,3,4,5,6,7,8,9,10,11,12,13,14]
plt.xticks(x_values,label)

plt.legend()

# Display the plot
plt.savefig("reactive.pdf")
