package cuttings
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 * Date: 20.11.14
 */
//@CompileStatic
class CuttingsService {

    private final ConfigObject config
    Map<Integer, VOCutting> mapCuttings = null

    CuttingsService(ConfigObject config) {
        this.config = config
    }

    public List<String> getYears() {
        Set<String> yeasSet = new HashSet<>()
        getCuttings().each {
            yeasSet.add(it.year)
        }

        def list = yeasSet.asList()
        list.sort()
        list
    }

    def List<VOCutting> getCuttingsByYear(final String year) {
        getCuttings().findAll { it.year == year }
    }

    private Collection<VOCutting> getCuttings() {
        if (!mapCuttings) {
            initCuttings()
        }

        mapCuttings.values()
    }

    def initCuttings() {
        mapCuttings = new HashMap<>()
        config.cuttings.base.each { String cfgPath ->
            File path = new File(cfgPath)
            path.eachFileRecurse { File file ->
                if (file.name.endsWith(".mht")) {
                    def cutting = new VOCutting(file)
                    mapCuttings.put(cutting.id, cutting)
                }
            }
        }
    }
}
