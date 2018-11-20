package classes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class DiskScheduler {
    private int numSectors;
    private int numTracks;
    private final static String OUTPUT_PATH = "./out.txt";
    private int startPosition;
    private List<DiskRequest> diskRequests;

    public DiskScheduler(String filePath) {
        this();
        this.initializeData(filePath);
    }

    private DiskScheduler() {
        this.numSectors = 0;
        this.numTracks = 0;
        this.startPosition = 0;
        this.diskRequests = new ArrayList<>();
        try {
            new PrintWriter(OUTPUT_PATH).close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initializeData(String filePath) {
        File file = new File(filePath);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String currentLine;
            for (int i = 0; (currentLine = reader.readLine()) != null; i++) {
                switch (i) {
                    case 0:
                        numSectors = Integer.valueOf(currentLine.replaceAll("[^0-9]", "")) - 1;
                        break;
                    case 1:
                        numTracks = Integer.valueOf(currentLine.replaceAll("[^0-9]", ""));
                        break;
                    case 2:
                        startPosition = Integer.valueOf(currentLine.replaceAll("[^0-9]", ""));
                        break;
                    default:
                        int[] fileValues = Stream.of(currentLine.substring(3).split(",")).mapToInt(Integer::parseInt).toArray();
                        diskRequests.add(new DiskRequest(currentLine.substring(0, 2), fileValues[0], fileValues[1], fileValues[2], fileValues[3]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<DiskRequest> cloneRequestQueue() {
        List<DiskRequest> clone = new ArrayList<>();
        for (DiskRequest request : this.diskRequests)
            clone.add(request);
        return clone;
    }

    public void firstComeFirstServed() {
        List<DiskRequest> diskRequests = cloneRequestQueue();
        int elapsedTime = 0;
        float totalAccessTime = 0;
        float totalWaitingTime = 0;
        DecimalFormat decimal = new DecimalFormat("#.##");
        int lastLatency = startPosition;
        int numRequests = diskRequests.size();
        while (!diskRequests.isEmpty()) {
            for (int i = 0; i < diskRequests.size(); i++) {
                int currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
                if (elapsedTime >= diskRequests.get(i).getArrivalTime()) {
//                    System.out.println(diskRequests.get(i) + " - " + currentAccessTime + " - " + totalAccessTime + " - " + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime()));
                    elapsedTime += currentAccessTime;
                    totalWaitingTime = totalWaitingTime + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
                    diskRequests.remove(i);
                    i--;
                }
            }
        }
        System.out.println("FCFS" + System.lineSeparator()
                + "-AccessTime=" + decimal.format(totalAccessTime / numRequests) + System.lineSeparator()
                + "-WaitingTime=" + decimal.format(totalWaitingTime / numRequests));
        this.writeData(OUTPUT_PATH, "FCFS", totalAccessTime / numRequests, totalWaitingTime / numRequests);
    }

    public void scan() {
        List<DiskRequest> diskRequests = cloneRequestQueue();
        int elapsedTime = 0;
        float totalAccessTime = 0;
        float totalWaitingTime = 0;
        int lastLatency = startPosition;
        int numRequests = diskRequests.size();
        DecimalFormat decimal = new DecimalFormat("#.##");
        diskRequests.sort(Comparator.naturalOrder());
        boolean isReturning = false;
        int numOfRequests;
        while (!diskRequests.isEmpty()) {
            int i;
            int currentAccessTime = 0;
            numOfRequests = diskRequests.size();
            for (i = 0; i < diskRequests.size(); i++) {
                if (elapsedTime >= diskRequests.get(i).getArrivalTime() && diskRequests.get(i).getLatency() >= lastLatency) {
                    if (isReturning) {
                        currentAccessTime = diskRequests.get(i).getSeekTime() + diskRequests.get(i).getTransferTime() + lastLatency + diskRequests.get(i).getLatency();
                        isReturning = false;
                    } else
                        currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
//                    System.out.println(diskRequests.get(i) + " - " + currentAccessTime + " - " + totalAccessTime + " - " + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime()));
                    elapsedTime += currentAccessTime;
                    totalWaitingTime += Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
                    diskRequests.remove(i);
                    i--;
                }
            }
            i--;
            if (!diskRequests.isEmpty()) isReturning = true;
            for (; i > -1; i--) {
                if (elapsedTime >= diskRequests.get(i).getArrivalTime() && diskRequests.get(i).getLatency() <= lastLatency) {
                    if (isReturning) {
                        currentAccessTime = ((this.numSectors - lastLatency) + (this.numSectors - diskRequests.get(i).getLatency())) + diskRequests.get(i).getSeekTime() + diskRequests.get(i).getTransferTime();
                        isReturning = false;
                    } else
                        currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
//                    System.out.println(diskRequests.get(i) + " - " + currentAccessTime + " - " + totalAccessTime + " - " + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime()));
                    elapsedTime += currentAccessTime;
                    totalWaitingTime = totalWaitingTime + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
                    diskRequests.remove(i);
                    i++;
                }
            }
            i++;
            if (!diskRequests.isEmpty()) isReturning = true;
            if (numOfRequests == diskRequests.size())
                elapsedTime++;
        }
        System.out.println("SCAN" + System.lineSeparator()
                + "-AccessTime=" + decimal.format(totalAccessTime / numRequests) + System.lineSeparator()
                + "-WaitingTime=" + decimal.format(totalWaitingTime / numRequests));
        this.writeData(OUTPUT_PATH, "SCAN", totalAccessTime / numRequests, totalWaitingTime / numRequests);
    }

    public void cScan() {
        List<DiskRequest> diskRequests = cloneRequestQueue();
        int elapsedTime = 0;
        float totalAccessTime = 0;
        float totalWaitingTime = 0;
        int lastLatency = startPosition;
        int numRequests = diskRequests.size();
        DecimalFormat decimal = new DecimalFormat("#.##");
        diskRequests.sort(Comparator.naturalOrder());
        boolean isReturning = false;
        int currentAccessTime = 0;
        while (!diskRequests.isEmpty()) {
            int i;
            for (i = 0; i < diskRequests.size(); i++) {
                if (elapsedTime >= diskRequests.get(i).getArrivalTime() && diskRequests.get(i).getLatency() >= lastLatency) {
                    if (isReturning) {
                        currentAccessTime += (diskRequests.get(i).getSeekTime() + diskRequests.get(i).getTransferTime() + diskRequests.get(i).getLatency());
                        isReturning = false;
                    } else {
                        currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
                    }
                    elapsedTime += currentAccessTime;
                    totalWaitingTime += Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
//                    System.out.println(diskRequests.get(i) + " - " + currentAccessTime + " - " + totalAccessTime + " - " + totalWaitingTime);
                    diskRequests.remove(i);
                    i--;
                }
            }
            if (!diskRequests.isEmpty()) {
                currentAccessTime = ((this.numSectors - lastLatency) + this.numSectors);
                isReturning = true;
                lastLatency = 0;
            }
        }
        System.out.println("C-SCAN" + System.lineSeparator()
                + "-AccessTime=" + decimal.format(totalAccessTime / numRequests) + System.lineSeparator()
                + "-WaitingTime=" + decimal.format(totalWaitingTime / numRequests));
        this.writeData(OUTPUT_PATH, "C-SCAN", totalAccessTime / numRequests, totalWaitingTime / numRequests);
    }

    public void cLook() {
        List<DiskRequest> diskRequests = cloneRequestQueue();
        int elapsedTime = 0;
        float totalAccessTime = 0;
        float totalWaitingTime = 0;
        int lastLatency = startPosition;
        int numRequests = diskRequests.size();
        DecimalFormat decimal = new DecimalFormat("#.##");
        diskRequests.sort(Comparator.naturalOrder());
        boolean isReturning = false;
        int currentAccessTime = 0;
        while (!diskRequests.isEmpty()) {
            int i;
            for (i = 0; i < diskRequests.size(); i++) {
                if (elapsedTime >= diskRequests.get(i).getArrivalTime() && diskRequests.get(i).getLatency() >= lastLatency) {
                    currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
                    elapsedTime += currentAccessTime;
                    totalWaitingTime += Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
                    diskRequests.remove(i);
                    i--;
                }
            }
            if (!diskRequests.isEmpty()) {
                currentAccessTime = diskRequests.get(0).getAccessTime(lastLatency);
                elapsedTime += currentAccessTime;
                totalWaitingTime += Math.abs(totalAccessTime - diskRequests.get(0).getArrivalTime());
                totalAccessTime += currentAccessTime;
                lastLatency = diskRequests.get(0).getLatency();
                diskRequests.remove(0);
                i--;
            }
        }
        System.out.println("C-LOOK" + System.lineSeparator()
                + "-AccessTime=" + decimal.format(totalAccessTime / numRequests) + System.lineSeparator()
                + "-WaitingTime=" + decimal.format(totalWaitingTime / numRequests));
        this.writeData(OUTPUT_PATH, "C-LOOK", totalAccessTime / numRequests, totalWaitingTime / numRequests);
    }

    public void shortestSeekTimeFirst() {
        List<DiskRequest> diskRequests = diskRequestsByProximity();
        int elapsedTime = 0;
        float totalAccessTime = 0;
        float totalWaitingTime = 0;
        int numRequests = diskRequests.size();
        DecimalFormat decimal = new DecimalFormat("#.##");
        int lastLatency = startPosition;
        while (!diskRequests.isEmpty()) {
            for (int i = 0; i < diskRequests.size(); i++) {
                int currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
                if (elapsedTime >= diskRequests.get(i).getArrivalTime()) {
//                    System.out.println(diskRequests.get(i) + " - " + currentAccessTime + " - " + totalAccessTime + " - " + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime()));
                    elapsedTime += currentAccessTime;
                    totalWaitingTime = totalWaitingTime + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
                    diskRequests.remove(i);
                    i--;
                }
            }
        }
        System.out.println("SSTF" + System.lineSeparator()
                + "-AccessTime=" + decimal.format(totalAccessTime / numRequests) + System.lineSeparator()
                + "-WaitingTime=" + decimal.format(totalWaitingTime / numRequests));
        this.writeData(OUTPUT_PATH, "SSTF", totalAccessTime / numRequests, totalWaitingTime / numRequests);
        diskRequests.forEach(System.out::println);
    }

    private void writeData(String filePath, String schedulingMethodName, float avgAccessTime, float avgNumRequests) {
        DecimalFormat decimal = new DecimalFormat("#.##");
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.APPEND)) {
            writer.write(schedulingMethodName + System.lineSeparator());
            writer.write("-AccessTime=" + decimal.format(avgAccessTime) + System.lineSeparator());
            writer.write("-WaitingTime=" + decimal.format(avgNumRequests) + System.lineSeparator());
            System.out.println("Escrito com sucesso em " + Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<DiskRequest> diskRequestsByProximity() {
        var diskRequests = cloneRequestQueue();
        int lastPosition = startPosition;
        int closestRequest = 0;
        int closestPosition = Integer.MAX_VALUE;
        for (int i = 0; i < diskRequests.size(); i++) {
            for (int j = i; j < diskRequests.size(); j++) {
                if (Math.abs(diskRequests.get(j).getLatency() - lastPosition) < closestPosition) {
                    closestRequest = j;
                    closestPosition = Math.abs(diskRequests.get(j).getLatency() - lastPosition);
                }
            }
            lastPosition = diskRequests.get(closestRequest).getLatency();
            Collections.swap(diskRequests, i, closestRequest);
            closestPosition = Integer.MAX_VALUE;
        }
        return diskRequests;
    }

    public void lastComeFirstServed() {
        List<DiskRequest> diskRequests = cloneRequestQueue();
        int elapsedTime = 0;
        float totalAccessTime = 0;
        float totalWaitingTime = 0;
        DecimalFormat decimal = new DecimalFormat("#.##");
        int lastLatency = startPosition;
        int numRequests = diskRequests.size();
        while (!diskRequests.isEmpty()) {
            for (int i = diskRequests.size() - 1; i >= 0; i--) {
                int currentAccessTime = diskRequests.get(i).getAccessTime(lastLatency);
                if (elapsedTime >= diskRequests.get(i).getArrivalTime()) {
//                    System.out.println(diskRequests.get(i) + " - " + currentAccessTime + " - " + totalAccessTime + " - " + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime()));
                    elapsedTime += currentAccessTime;
                    totalWaitingTime = totalWaitingTime + Math.abs(totalAccessTime - diskRequests.get(i).getArrivalTime());
                    totalAccessTime += currentAccessTime;
                    lastLatency = diskRequests.get(i).getLatency();
                    diskRequests.remove(i);
                }
            }
        }
        System.out.println("MY" + System.lineSeparator()
                + "-AccessTime=" + decimal.format(totalAccessTime / numRequests) + System.lineSeparator()
                + "-WaitingTime=" + decimal.format(totalWaitingTime / numRequests));
        this.writeData(OUTPUT_PATH, "MY", totalAccessTime / numRequests, totalWaitingTime / numRequests);
    }

    public void executeAllMethods() {
        this.firstComeFirstServed();
        this.shortestSeekTimeFirst();
        this.scan();
        this.cScan();
        this.cLook();
        this.lastComeFirstServed();
    }
}
