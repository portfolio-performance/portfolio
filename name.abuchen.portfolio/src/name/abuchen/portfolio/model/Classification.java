package name.abuchen.portfolio.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import com.google.protobuf.NullValue;

import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.model.proto.v1.PKeyValue;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.ColorConversion;

public class Classification implements Named
{
    private static final String PORTFOLIO_CLASSIFICATION_KEY = "portfolioClassificationKey"; //$NON-NLS-1$

    public static class Assignment
    {
        private InvestmentVehicle investmentVehicle;
        private int weight;
        private int rank;
        private Map<String, Object> data;

        public Assignment()
        {
            // needed for xstream de-serialization
        }

        public Assignment(InvestmentVehicle vehicle)
        {
            this(vehicle, ONE_HUNDRED_PERCENT);
        }

        public Assignment(InvestmentVehicle vehicle, int weight)
        {
            this.weight = weight;
            this.investmentVehicle = vehicle;
        }

        public int getWeight()
        {
            return weight;
        }

        public void setWeight(int weight)
        {
            this.weight = weight;
        }

        public InvestmentVehicle getInvestmentVehicle()
        {
            return investmentVehicle;
        }

        public int getRank()
        {
            return rank;
        }

        public void setRank(int rank)
        {
            this.rank = rank;
        }

        public Object setData(String key, Object object)
        {
            if (data == null)
                data = new HashMap<>();

            return object == null ? data.remove(key) : data.put(key, object);
        }

        public Object getData(String key)
        {
            if (data == null)
                return null;

            return data.get(key);
        }

        public Assignment copyWith(InvestmentVehicle vehicle)
        {
            Assignment copy = new Assignment(vehicle);
            copy.setWeight(this.getWeight());
            copy.setRank(this.getRank());
            if (this.data != null)
                copy.data = new HashMap<>(this.data);
            return copy;
        }

        /* protobuf only */ List<PKeyValue> getData()
        {
            return toProtobuf(data);
        }

        /* protobuf only */ void setData(List<PKeyValue> list)
        {
            this.data = fromProtobuf(list);
        }
    }

    public static final int ONE_HUNDRED_PERCENT = 100 * Values.Weight.factor();
    public static final BigDecimal ONE_HUNDRED_PERCENT_BD = BigDecimal.valueOf(Values.Weight.factorize(100));

    public static final String UNASSIGNED_ID = "$unassigned$"; //$NON-NLS-1$
    public static final String VIRTUAL_ROOT = "$virtualroot$"; //$NON-NLS-1$

    private static final Random RANDOM = new Random();

    private String id;
    private String name;
    private String description;
    private String color;

    private Classification parent;
    private List<Classification> children = new ArrayList<>();
    private List<Assignment> assignments = new ArrayList<>();

    private int weight;
    private int rank;

    private Map<String, Object> data;

    public Classification()
    {
        // needed for xstream de-serialization
    }

    public Classification(String id, String name)
    {
        this(null, id, name);
    }

    public Classification(Classification parent, String id, String name, String color)
    {
        this.parent = parent;
        this.id = id;
        this.name = name;
        this.color = color;

        if (color == null)
        {
            Random r = new Random();
            this.color = '#' + Integer.toHexString(((r.nextInt(128) + 127) << 16) //
                            | ((r.nextInt(128) + 127) << 8) //
                            | (r.nextInt(128) + 127));
        }

        this.weight = ONE_HUNDRED_PERCENT;
    }

    public Classification(Classification parent, String id, String name)
    {
        this(parent, id, name, null);
    }

    public void setKey(String key)
    {
        setData(PORTFOLIO_CLASSIFICATION_KEY, key);
    }

    public String getKey()
    {
        return Objects.toString(getData(PORTFOLIO_CLASSIFICATION_KEY), ""); //$NON-NLS-1$
    }

    public String getId()
    {
        return id;
    }

    /* package */void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getNote()
    {
        return description;
    }

    @Override
    public void setNote(String note)
    {
        this.description = note;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public Classification getParent()
    {
        return parent;
    }

    public void setParent(Classification parent)
    {
        this.parent = parent;
    }

    public List<Classification> getChildren()
    {
        return children;
    }

    public void addChild(Classification classification)
    {
        children.add(classification);
    }

    public List<Assignment> getAssignments()
    {
        return assignments;
    }

    public void addAssignment(Assignment assignment)
    {
        assignments.add(assignment);
    }

    public void removeAssignment(Assignment assignment)
    {
        assignments.remove(assignment);
    }

    public void clearAssignments()
    {
        assignments.clear();
    }

    public int getWeight()
    {
        return weight;
    }

    public int getChildrenWeight()
    {
        int sum = 0;
        for (Classification child : children)
            sum += child.getWeight();
        return sum;
    }

    public void setWeight(int weight)
    {
        this.weight = weight;
    }

    public int getRank()
    {
        return rank;
    }

    public void setRank(int rank)
    {
        this.rank = rank;
    }

    public Object setData(String key, Object object)
    {
        if (data == null)
            data = new HashMap<>();

        return object == null ? data.remove(key) : data.put(key, object);
    }

    public Object getData(String key)
    {
        if (data == null)
            return null;

        return data.get(key);
    }

    /* protobuf only */ List<PKeyValue> getData()
    {
        return toProtobuf(data);
    }

    /* protobuf only */ void setData(List<PKeyValue> list)
    {
        this.data = fromProtobuf(list);
    }

    public String getPathName(boolean includeParent, int limit)
    {
        LinkedList<Classification> path = getPath();

        // remove root node
        if (!includeParent && path.size() > 1)
            path.removeFirst();

        // short circuit
        if (path.size() == 1)
            return path.get(0).getName();

        // add as many elements from left to right as possible

        int available = limit;

        StringBuilder leftBuffer = new StringBuilder();
        StringBuilder rightBuffer = new StringBuilder();

        int left = 0;
        int right = 0;

        while (left + right < path.size())
        {
            if ((left + right) % 2 == 0) // start right
            {
                // do right
                Classification c = path.get(path.size() - 1 - right);
                available -= c.getName().length();

                if (available < 0)
                    break;

                if (rightBuffer.length() > 0)
                    rightBuffer.insert(0, " » "); //$NON-NLS-1$
                rightBuffer.insert(0, c.getName());
                right++;
            }
            else
            {
                // do left
                Classification c = path.get(left);
                available -= c.getName().length();

                if (available < 0)
                    break;

                if (leftBuffer.length() > 0)
                    leftBuffer.append(" » "); //$NON-NLS-1$
                leftBuffer.append(c.getName());
                left++;
            }
        }

        if (left + right == path.size())
            return leftBuffer.toString() + " » " + rightBuffer.toString(); //$NON-NLS-1$
        else
            return leftBuffer.toString() + " ... " + rightBuffer.toString(); //$NON-NLS-1$
    }

    public String getPathName(boolean includeParent)
    {
        LinkedList<Classification> path = getPath();
        if (!includeParent && path.size() > 1)
            path.removeFirst();

        StringBuilder buf = new StringBuilder();

        for (Classification c : path)
        {
            if (buf.length() > 0)
                buf.append(" » "); //$NON-NLS-1$

            buf.append(c.getName());
        }

        return buf.toString();
    }

    private LinkedList<Classification> getPath()
    {
        LinkedList<Classification> path = new LinkedList<>();

        Classification c = this;
        while (c != null)
        {
            path.addFirst(c);
            c = c.getParent();
        }

        return path;
    }

    public List<Classification> getTreeElements()
    {
        List<Classification> answer = new ArrayList<>();

        LinkedList<Classification> stack = new LinkedList<>();

        List<Classification> list = new ArrayList<>(getChildren());
        list.sort((r, l) -> Integer.compare(r.getRank(), l.getRank()));
        stack.addAll(list);

        while (!stack.isEmpty())
        {
            Classification c = stack.pop();
            answer.add(c);
            list = new ArrayList<>(c.getChildren());
            list.sort((r, l) -> Integer.compare(r.getRank(), l.getRank()));
            stack.addAll(0, list);
        }

        return answer;
    }

    public List<Classification> getPathToRoot()
    {
        LinkedList<Classification> path = new LinkedList<>();

        Classification item = this;
        while (item != null)
        {
            path.addFirst(item);
            item = item.getParent();
        }

        return path;
    }

    public void assignRandomColors()
    {
        float hue = RANDOM.nextFloat() * 360f;
        float saturation = (RANDOM.nextFloat() * 0.5f) + 0.3f;
        float brightness = (RANDOM.nextFloat() * 0.4f) + 0.5f;

        assignRandomColors(hue, saturation, brightness);
    }

    /* package */void assignRandomColors(float hue, float saturation, float brightness)
    {
        if (children.isEmpty())
            return;

        Collections.sort(children, (c1, c2) -> Integer.compare(c2.getRank(), c1.getRank()));

        int size = children.size();
        float step = 360f / (float) size;

        int index = 0;
        for (Classification child : children)
        {
            float h = (hue + (step * index)) % 360f;

            child.setColor(ColorConversion.toHex(h, saturation, brightness));
            child.cascadeColorDown(h, saturation, brightness);
            index++;
        }
    }

    public void cascadeColorDown()
    {
        if (children.isEmpty())
            return;

        float[] hsb = ColorConversion.toHSB(color);
        cascadeColorDown(hsb[0], hsb[1], hsb[2]);
    }

    private void cascadeColorDown(float hue, float saturation, float brightness)
    {
        if (children.isEmpty())
            return;

        float childSaturation = Math.max(0f, saturation - 0.1f);
        float childBrightness = Math.min(1f, brightness + 0.1f);

        for (Classification child : children)
        {
            child.setColor(ColorConversion.toHex(hue, childSaturation, childBrightness));
            child.cascadeColorDown(hue, childSaturation, childBrightness);
        }
    }

    public void accept(Visitor visitor)
    {
        visitor.visit(this);

        getChildren().stream() //
                        .sorted((r, l) -> Integer.compare(r.getRank(), l.getRank()))
                        .forEach(child -> child.accept(visitor));

        for (Assignment assignment : new ArrayList<Assignment>(assignments))
            visitor.visit(this, assignment);
    }

    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Recursively creates a copy of the classification including all
     * assignments with newly generated UUIDs.
     */
    public Classification copy()
    {
        Classification copy = new Classification(null, UUID.randomUUID().toString(), this.name, this.color);
        copy.rank = this.rank;
        copy.weight = this.weight;

        if (this.data != null)
            copy.data = new HashMap<>(this.data);

        for (Classification classification : children)
        {
            Classification c = classification.copy();
            c.setParent(copy);
            copy.addChild(c);
        }

        for (Assignment assignment : assignments)
        {
            Assignment a = assignment.copyWith(assignment.getInvestmentVehicle());
            copy.addAssignment(a);
        }

        return copy;
    }

    /**
     * Returns a classification that is a copy of this classification, but all
     * assignments are merged as direct children.
     */
    public Classification flattenAssignments()
    {
        var result = new Classification(null, id, name, this.color);
        result.rank = this.rank;
        result.weight = this.weight;
        if (this.data != null)
            result.data = new HashMap<>(this.data);

        var stack = new LinkedList<Classification>();
        stack.add(this);

        while (!stack.isEmpty())
        {
            var c = stack.pop();
            stack.addAll(c.getChildren());

            for (Assignment assignment : c.getAssignments())
            {
                if (assignment.getWeight() == ONE_HUNDRED_PERCENT)
                {
                    result.addAssignment(assignment);
                }
                else
                {
                    Optional<Assignment> existing = result.assignments.stream()
                                    .filter(a -> a.getInvestmentVehicle() == assignment.getInvestmentVehicle())
                                    .findAny();

                    if (existing.isPresent())
                    {
                        Assignment merged = new Assignment(assignment.getInvestmentVehicle());
                        merged.weight = existing.get().getWeight() + assignment.getWeight();

                        result.removeAssignment(existing.get());
                        result.addAssignment(merged);
                    }
                    else
                    {
                        result.addAssignment(assignment);
                    }
                }
            }
        }

        return result;
    }

    private static List<PKeyValue> toProtobuf(Map<String, Object> data)
    {
        if (data == null || data.isEmpty())
            return Collections.emptyList();

        List<PKeyValue> answer = new ArrayList<>();

        data.entrySet().stream().forEach(entry -> {
            PKeyValue.Builder newEntry = PKeyValue.newBuilder();
            newEntry.setKey(entry.getKey());

            Object value = entry.getValue();
            if (value == null)
                newEntry.setValue(PAnyValue.newBuilder().setNullValue(NullValue.NULL_VALUE_VALUE).build());
            else if (value instanceof String s)
                newEntry.setValue(PAnyValue.newBuilder().setString(s));
            else if (value instanceof Boolean b)
                newEntry.setValue(PAnyValue.newBuilder().setBool(b));
            else if (value instanceof Integer i)
                newEntry.setValue(PAnyValue.newBuilder().setInt32(i));
            else
                throw new UnsupportedOperationException(value.getClass().getName());

            answer.add(newEntry.build());
        });

        return answer;
    }

    private static Map<String, Object> fromProtobuf(List<PKeyValue> list)
    {
        if (list.isEmpty())
            return null;

        Map<String, Object> data = new HashMap<>();

        list.forEach(entry -> {

            String key = entry.getKey();

            Object value = null;

            if (entry.getValue().hasString())
                value = entry.getValue().getString();
            else if (entry.getValue().hasBool())
                value = entry.getValue().getBool();
            else if (entry.getValue().hasInt32())
                value = entry.getValue().getInt32();

            // ignore unknown types upon reading

            data.put(key, value);
        });

        return data;
    }

}
