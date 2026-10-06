package com.sezercan.namazvakti;

import static org.junit.Assert.*;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SyncTest {

    @org.junit.Before
    public void offline() {
        Sync.server = "http://127.0.0.1:9/";
    }


    private final Context c = RuntimeEnvironment.getApplication();

    @Test
    public void codeNormalization() {
        assertEquals("ABCD2345", Sync.normalize("abcd-2345"));
        assertEquals("ABCD2345", Sync.normalize(" ABCD 2345 "));
        assertNull(Sync.normalize("ABCD-234"));     // kısa
        assertNull(Sync.normalize("ABCD-O345"));    // O harfi kodda yok
        String code = Sync.newCode();
        assertEquals(code, Sync.normalize(Sync.pretty(code)));
        assertTrue(Sync.topic(code).matches("nv-[0-9a-f]{32}"));
        assertEquals(Sync.topic("ABCD2345"), Sync.topic("ABCD2345"));
    }

    @Test
    public void habitsAndSnapshot() throws Exception {
        String today = Habits.dayKey(Times.today());
        Habits.set(c, today, PrayerTimes.OGLE, true);
        Habits.set(c, today, PrayerTimes.IMSAK, true);
        Habits.set(c, today, PrayerTimes.IMSAK, false);
        assertTrue(Habits.prayed(c, today, PrayerTimes.OGLE));
        assertFalse(Habits.prayed(c, today, PrayerTimes.IMSAK));
        assertEquals(1, Habits.count(Habits.mask(c, today)));

        Sync.setMyName(c, "Sezer");
        JSONObject snap = new JSONObject(Sync.snapshot(c));
        assertEquals("Sezer", snap.getString("ad"));
        assertEquals(7, snap.getJSONObject("g").length());
        assertEquals(1 << PrayerTimes.OGLE, snap.getJSONObject("g").getInt(today));
    }

    @Test
    public void mergeKeepsNewestFriendSnapshotAndSkipsMine() throws Exception {
        Sync.pair(c, "ABCD2345");
        String today = Habits.dayKey(Times.today());
        String mine = new JSONObject(Sync.snapshot(c)).toString();
        String friendOld = "{\"v\":1,\"id\":\"f1\",\"ad\":\"Ahmet\",\"t\":100,\"g\":{\"" + today + "\":1}}";
        String friendNew = "{\"v\":1,\"id\":\"f1\",\"ad\":\"Ahmet\",\"t\":200,\"g\":{\"" + today + "\":37}}";
        int changed = Sync.mergeLines(c, Arrays.asList(
                ev(friendOld), ev(mine), ev(friendNew), "{\"event\":\"open\"}", "", ev("bozuk{")));
        assertEquals(2, changed);
        Sync.Friend f = Sync.friendsIncludingStale(c).get(0);
        assertEquals("Ahmet", f.name);
        assertEquals(200, f.updated);
        assertEquals(37, f.mask(today));
        // Daha eski bir mesaj sonradan gelse de yenisini ezmez.
        Sync.mergeLines(c, Arrays.asList(ev(friendOld)));
        assertEquals(37, Sync.friendsIncludingStale(c).get(0).mask(today));

        // Yeniden kurulum: aynı isim, yeni kimlik → tek kişi, yenisi gösterilir.
        long now = System.currentTimeMillis();
        String reinstalled = "{\"v\":1,\"id\":\"f2\",\"ad\":\"ahmet\",\"t\":" + now + ",\"g\":{\"" + today + "\":4}}";
        String stale = "{\"v\":1,\"id\":\"f3\",\"ad\":\"Eski\",\"t\":1000,\"g\":{}}";
        Sync.mergeLines(c, Arrays.asList(ev(reinstalled), ev(stale)));
        assertEquals(1, Sync.friends(c).size());
        assertEquals(4, Sync.friends(c).get(0).mask(today));

        Sync.unpair(c);
        assertFalse(Sync.paired(c));
        assertTrue(Sync.friends(c).isEmpty());
    }

    @Test
    public void everyPoemIsComplete() {
        Poems.Poem[] all = Poems.all(c);
        assertTrue(all.length >= 30);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Poems.Poem p : all) {
            assertFalse(p.verse.isEmpty());
            assertFalse(p.poet.isEmpty());
            assertFalse(p.info.isEmpty());
            assertTrue("tekrar: " + p.verse, seen.add(p.verse));
        }
        // Art arda iki gün farklı mısra.
        java.util.Calendar d = Times.today();
        Poems.Poem a = Poems.forDay(c, d);
        d.add(java.util.Calendar.DAY_OF_MONTH, 1);
        assertNotSame(a, Poems.forDay(c, d));
    }

    private static String ev(String message) throws Exception {
        JSONObject o = new JSONObject();
        o.put("event", "message");
        o.put("message", message);
        return o.toString();
    }
}
