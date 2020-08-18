package compress.memory;

public class Jungle {
    private final Territory[] boundary;
    private Territory establishedTerritoriesStack = null;
    private long cultures = 0;

    public Jungle() {
        boundary = new Territory[Constants.DECIMAL_SYS];
    }

    public Territory getTerritoriesList() {
        return establishedTerritoriesStack;
    }

    public long getCultures() {
        return cultures;
    }

    public long getPopulation() {
        long population = 0;
        Territory t = establishedTerritoriesStack;

        while (t != null) {
            population += t.tribe.inhabitants;
            t = t.next;
        }

        return population;
    }

    public void nuke() {
        Territory t = establishedTerritoriesStack;

        while (t != null) {
            t.tribe.inhabitants = 0;
            t.tribe.flag = false;
            t = t.next;
        }
    }

    public Tribe establish(long culture) {
        final long the_culture = culture;
        Territory territory;
        Pathway pathway;
        int way = (int) (culture % Constants.DECIMAL_SYS);

        culture /= Constants.DECIMAL_SYS;
        if (boundary[way] == null) {
            territory = new Territory();
            boundary[way] = territory;
        } else {
            territory = boundary[way];
        }

        while (culture > 0) {
            way = (int) (culture % Constants.DECIMAL_SYS);
            culture /= Constants.DECIMAL_SYS;
            pathway = territory.pathways;
            while (pathway != null && pathway.trailSign != way) {
                pathway = pathway.next;
            }
            if (pathway == null) {
                pathway = new Pathway((byte) way, new Territory());
                pathway.next = territory.pathways;
                territory.pathways = pathway;
            }
            territory = pathway.leadTo;
        }

        if (territory.tribe == null) {
            territory.tribe = new Tribe(the_culture);
            territory.next = establishedTerritoriesStack;
            establishedTerritoriesStack = territory;
            cultures++;
            return territory.tribe;
        } else {
            return null;
        }
    }

    public Tribe find(long culture) {
        Territory territory;
        Pathway pathway;
        int way = (int) (culture % Constants.DECIMAL_SYS);

        culture /= Constants.DECIMAL_SYS;
        if (boundary[way] == null)
            return null;
        else
            territory = boundary[way];

        while (culture > 0) {
            way = (int) (culture % Constants.DECIMAL_SYS);
            culture /= Constants.DECIMAL_SYS;
            pathway = territory.pathways;
            while (pathway != null && pathway.trailSign != way) {
                pathway = pathway.next;
            }
            if (pathway == null)
                return null;
            else
                territory = pathway.leadTo;
        }

        return territory.tribe;
    }

    public Tribe go(long culture) {
        Territory territory;
        Pathway pathway;
        int way = (int) (culture % Constants.DECIMAL_SYS);

        culture /= Constants.DECIMAL_SYS;
        territory = boundary[way];

        while (culture > 0) {
            way = (int) (culture % Constants.DECIMAL_SYS);
            culture /= Constants.DECIMAL_SYS;
            pathway = territory.pathways;
            while (pathway.trailSign != way) {
                pathway = pathway.next;
            }
            territory = pathway.leadTo;
        }

        return territory.tribe;
    }

    public static class Tribe {
        final int culture;
        int inhabitants = 0;
        public boolean flag = false;

        Tribe(long culture) {
            this.culture = fitInteger(culture);
        }

        int fitInteger(long l) {
            return (int) (l > Integer.MAX_VALUE ? (l - Math.pow(256, 4)) : l);
        }

        public void oneMovedIn() {
            inhabitants++;
        }

        public long getCulture() {
            return culture < 0 ? (long) (culture + Math.pow(256, 4)) : culture;
        }

        public int getInhabitants() {
            return inhabitants;
        }
    }

    public static class Territory {
        Tribe tribe = null;
        Pathway pathways = null;
        Territory next = null;

        Territory() {
        }

        public Tribe tribe() {
            return tribe;
        }

        public Territory visitNext() {
            return next;
        }
    }

    static class Pathway {
        byte trailSign;
        Territory leadTo;
        Pathway next = null;

        Pathway(byte trailSign, Territory leadTo) {
            this.trailSign = trailSign;
            this.leadTo = leadTo;
        }
    }

    private static class Constants {
        static final int DECIMAL_SYS = 10;
    }
}
