package prog11;

import prog05.ArrayQueue;
import java.util.*;

public class NotGPT implements SearchEngine {
    // start of part 1
    HardDisk pageDisk = new HardDisk();
    HardDisk wordDisk = new HardDisk();
    Map<String,Long> urlToIndex = new TreeMap<>();
    Map<String, Long> wordToIndex = new HashMap<>();
    public Long indexPage(String url) {
        Long index = pageDisk.newFile();
        InfoFile urlFile = new InfoFile(url);
        pageDisk.put(index, urlFile);
        urlToIndex.put(url, index);
        System.out.println("indexing page " + index  + " " + urlFile);
        return urlToIndex.get(url);
    }
    public Long indexWord(String word) {
        Long index = wordDisk.newFile();
        InfoFile wordFile = new InfoFile(word);
        wordDisk.put(index, wordFile);
        wordToIndex.put(word, index);
        System.out.println("indexing word " + index + " " + wordFile);
        return wordToIndex.get(word);
    }
    public void collect(Browser browser, List<String> startingURLs) {
        Queue<Long> indexQueue = new ArrayQueue<>();
        System.out.println("starting pages " + startingURLs.toString());
        for (String url : startingURLs) {
            if (urlToIndex.get(url) == null) {
                indexQueue.offer(indexPage(url));
            }
        }
        while (!indexQueue.isEmpty()) {
            System.out.println("queue " + indexQueue.toString());
            long index = indexQueue.poll();
            InfoFile pageFile = pageDisk.get(index);
            System.out.println("dequeued " + pageFile);
            boolean loaded = browser.loadPage(pageFile.data);
            if (loaded) {
                Set<String> urlsOnPage = new HashSet<>();
                List<String> urlList = browser.getURLs();
                System.out.println("urls " + urlList.toString());
                for (String url : urlList) {
                    if (!urlsOnPage.contains(url)) {
                        urlsOnPage.add(url);
                        if (urlToIndex.get(url) == null) {
                            indexQueue.offer(indexPage(url));
                        }
                    }
                    if (!pageFile.indices.contains(urlToIndex.get(url))) {
                        pageFile.indices.add(urlToIndex.get(url));
                    }
                } // end of the for loop
            } // end of the conditional checking if the page loaded
            pageDisk.put(index, pageFile);
            System.out.println("updated page file " + pageFile.toString());
            List<String> words = browser.getWords();
            Set<String> wordsOnPage = new HashSet<>();
            System.out.println("words "+ words.toString());
            for (String word : words) {
                if (!wordsOnPage.contains(word)) {
                    wordsOnPage.add(word);
                    Long wordIndex = wordToIndex.get(word);
                    if (wordIndex == null) {
                        wordIndex = indexWord(word);
                    }
                    InfoFile wordFile = wordDisk.get(wordIndex);
                    wordFile.indices.add(index);
                    wordDisk.put(wordIndex, wordFile);
                    System.out.println("updated word file " + wordFile);
                }
            }
        } // end of the while loop
    } // end of the collect method
    // end of part 1

    // start of part 2
    void rankSlow (double defaultInfluence) {
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            // long index = entry.getKey();
            InfoFile file = entry.getValue();
            double influencePerIndex = file.influence / file.indices.size();
            for (Long i : file.indices) {
                pageDisk.get(i).influenceTemp += influencePerIndex;
            }
        }
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            // long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.influence = file.influenceTemp + defaultInfluence;
            file.influenceTemp = 0.0;
        }
    }
    void rankFast (double defaultInfluence) {
        List<Vote> votes = new ArrayList<>();
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            // long index = entry.getKey();
            InfoFile file = entry.getValue();
            for (Long i : file.indices) {
                Vote theVote = new Vote();
                theVote.index = i;
                theVote.vote = file.influence / file.indices.size();
                votes.add(theVote);
            }
        }
        Collections.sort(votes);
        Iterator<Vote> voteIterator = votes.iterator();
        Vote currentVote = voteIterator.next();
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.influence = 0.0;
            while (index == currentVote.index) {
                file.influence += currentVote.vote;
                /*
                try {
                    currentVote = voteIterator.next();
                } catch (NoSuchElementException e) {
                    break;
                }
                */
                if (voteIterator.hasNext()) {currentVote = voteIterator.next();}
                else {break;}
            }
            file.influence += defaultInfluence;
        }
    }
    public void rank(boolean fast) {
        int count = 0; // number of pages that have no links to other pages
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            // long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.influence = 1.0;
            file.influenceTemp = 0.0;
            if (file.indices.isEmpty()) {count++;}
        }
        double defaultInfluence = 1.0 * count / pageDisk.size();
        if (!fast) {
            for (int i = 0; i < 20; i++) {
                rankSlow(defaultInfluence);
            }
        }
        else {
            for (int i = 0; i < 20; i++) {
                rankFast(defaultInfluence);
            }
        }
    }
    private class Vote implements Comparable<Vote> {
        Long index;
        double vote;

        @Override
        public int compareTo(Vote o) {
            if (!this.index.equals(o.index)) {return Long.compare(this.index, o.index);}
            else {return Double.compare(this.vote, o.vote);}
        }
    }
    // end of part 2

    // start of part 3
    /** Check if all elements in an array of long are equal.
     @param array an array of numbers
     @return true if all are equal, false otherwise
     */
    private boolean allEqual (long[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i + 1] != array[i]) {return false;}
        }
        return true;
    }

    /** Get the largest element of an array of long.
     @param array an array of numbers
     @return largest element
     */
    private long getLargest (long[] array) {
        long largest = array[0];
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i + 1] > array[i]) {largest = array[i + 1];}
        }
        return largest;
    }

    /** If all the elements of currentPageIndex are equal,
     set each one to the next() of its Iterator,
     but if any Iterator hasNext() is false, just return false.

     Otherwise, do that for every element not equal to the largest element.

     Return true.

     @param currentPageIndex array of current page indices
     @param wordPageIndexIterators array of iterators with next page indices
     @return true if all page indices are updated, false otherwise
     */
    private boolean getNextPageIndices
    (long[] currentPageIndex, Iterator<Long>[] wordPageIndexIterators) {
        if (allEqual(currentPageIndex)) {
            for (int i = 0; i < currentPageIndex.length; i++) {
                if (!wordPageIndexIterators[i].hasNext()) {return false;}
                else {currentPageIndex[i] = wordPageIndexIterators[i].next();}
            }
        } else {
            for (int i = 0; i < currentPageIndex.length; i++) {
                if (currentPageIndex[i] != getLargest(currentPageIndex)) {
                    if (!wordPageIndexIterators[i].hasNext()) {return false;}
                    currentPageIndex[i] = wordPageIndexIterators[i].next();
                }
            }
        }
        return true;
    }

    public String[] search(List<String> searchWords, int numResults) {
        for (int i = 0; i < searchWords.size(); i++) {
            if (!wordToIndex.containsKey(searchWords.get(i))) {
                searchWords.remove(i);
            }
        }
        if (searchWords.isEmpty()) {
            return new String[0];
        }
        Iterator<Long>[] wordPageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];
        long[] currentPageIndex;
        PageComparator pageComparator = new PageComparator();
        PriorityQueue<Long> bestPageIndices;
        bestPageIndices = new PriorityQueue<>(pageComparator);
        for (String word : searchWords) {
            Long index = wordToIndex.get(word);
            InfoFile page = wordDisk.get(index);
            wordPageIndexIterators[searchWords.indexOf(word)] = page.indices.iterator();
        }
        currentPageIndex = new long[searchWords.size()];
        while (getNextPageIndices(currentPageIndex, wordPageIndexIterators)) {
            if (allEqual(currentPageIndex)) {
                // we have found a match
                System.out.println(pageDisk.get(currentPageIndex[0]).data);
                if (bestPageIndices.size() < numResults) {
                    bestPageIndices.offer(currentPageIndex[0]);
                } else if (pageComparator.compare(currentPageIndex[0], bestPageIndices.peek()) > 0) {
                    // use peek() and pageComparator to determine if matching page should go into the queue
                    bestPageIndices.poll();
                    bestPageIndices.offer(currentPageIndex[0]);
                }
            }
        }
        String[] results = new String[bestPageIndices.size()];
        for (int i = results.length - 1; i >= 0; i--) {
            results[i] = pageDisk.get(bestPageIndices.poll()).data;
        }
        return results;
    }
    private class PageComparator implements Comparator<Long> {
        @Override
        public int compare(Long pageIndex1, Long pageIndex2) {
            return Double.compare(pageDisk.get(pageIndex1).influence, pageDisk.get(pageIndex2).influence);
        }
    }
    // end of part 3
}
