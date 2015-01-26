package com.levelupstudio.recyclerview;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

/**
 * A class equivalent to {@link android.widget.ExpandableListView ExpandableListView} with the {@code RecyclerView} features.
 * <p>You must use an {@link com.levelupstudio.recyclerview.ExpandableRecyclerView.ExpandableAdapter} instead of a {@link android.support.v7.widget.RecyclerView.Adapter}
 * and an {@link com.levelupstudio.recyclerview.ExpandableRecyclerView.ExpandableViewHolder} instead of a {@link android.support.v7.widget.RecyclerView.ViewHolder}.</p>
 * <p>Only one element can be expanded at a time.</p>
 *
 * @author Created by robUx4 on 02/10/2014.
 */
public class ExpandableRecyclerView extends RecyclerViewWithHeader {

	private ExpandableListView.OnGroupExpandListener onGroupExpandListener;
	private ExpandableListView.OnGroupCollapseListener onGroupCollapseListener;
	private OnGroupClickListener onGroupClickListener;
	/** {@link android.support.v7.widget.RecyclerView.ItemAnimator} used for normal operations */
	private ItemAnimator mNormalItemAnimator;

	private int selectedGroup = RecyclerView.NO_POSITION;
	private Parcelable selectedStableId;

	private boolean firstLayoutPassed;

	/**
	 * Interface definition for a callback to be invoked when a group in this expandable list has been clicked.
	 * Similar to {@link android.widget.ExpandableListView.OnGroupClickListener}.
	 */
	public interface OnGroupClickListener {
		/**
		 * Callback method to be invoked when a group in this expandable list has
		 * been clicked.
		 *
		 * @param parent        The ExpandableRecyclerView where the click happened
		 * @param v             The view within the expandable list that was clicked
		 * @param groupPosition The group position that was clicked
		 * @param id            The row id of the group that was clicked, always 0
		 * @return True if the click was handled
		 */
		boolean onGroupClick(ExpandableRecyclerView parent, View v, int groupPosition, long id);
	}

	/**
	 * A ViewHolder describes an item view and metadata about its place within the ExpandableRecyclerView.
	 *
	 * <p>{@link ExpandableAdapter} implementations should subclass ExpandableViewHolder and add fields for caching
	 * potentially expensive {@link View#findViewById(int)} results.</p>
	 *
	 * <p>While {@link LayoutParams} belong to the {@link LayoutManager},
	 * {@link ViewHolder ViewHolders} belong to the adapter. Adapters should feel free to use
	 * their own custom ViewHolder implementations to store data that makes binding view contents
	 * easier. Implementations should assume that individual item views will hold strong references
	 * to {@code ExpandableViewHolder} objects and that <{@code ExpandableRecyclerView} instances may hold
	 * strong references to extra off-screen item views for caching purposes</p>
	 */
	public static class ExpandableViewHolder extends ViewHolder implements OnClickListener {
		private ExpandHandler expandHandler;
		private boolean expanded;
		boolean isSelected;

		public ExpandableViewHolder(@NonNull View itemView) {
			super(itemView);
			itemView.setOnClickListener(this);
		}

		void setExpandHandler(ExpandHandler expandHandler) {
			this.expandHandler = expandHandler;
		}

		/**
		 * Handle the click on the View. By default doesn't do anything.
		 *
		 * @return {@code true} if the click has been handled.
		 */
		protected boolean onViewClicked(View view) {
			return false;
		}

		/**
		 * Called when the ViewHolder is about to be expanded/collapsed.
		 *
		 * @see #isExpanded()
		 */
		protected void onExpandedChanged() {
		}

		/**
		 * Indicates whether the ViewHolder is expanded or not.
		 *
		 * @see #canExpand()
		 */
		public final boolean isExpanded() {
			return expanded;
		}

		/**
		 * Indicates whether the ViewHolder is selected or not.
		 *
		 * @see #setSelectedGroup(int)
		 */
		public final boolean isSelected() {
			return isSelected;
		}

		@Override
		public final void onClick(View view) {
			if (!onViewClicked(view) && view == itemView && canExpand() && null != expandHandler) {
				expandHandler.onViewExpand(this);
			}
		}

		/**
		 * Indicates whether this ViewHolder can expand. By default it doesn't.
		 */
		protected boolean canExpand() {
			return false;
		}

		@Override
		public String toString() {
			return super.toString() + ',' + "expanded="+expanded + ',' + "selected="+isSelected;
		}
	}

	private static interface ExpandHandler {
		void onViewExpand(ExpandableViewHolder holder);
	}

	/**
	 * Base class for an ExpandableAdapter
	 *
	 * <p>Adapters provide a binding from an app-specific data set to views that are displayed
	 * within a {@link ExpandableRecyclerView}.</p>
	 */
	public static abstract class ExpandableAdapter<VH extends ExpandableViewHolder, T> extends Adapter<VH> implements ExpandHandler {
		public static final boolean DEBUG = BuildConfig.DEBUG && false;
		protected static final String LOG_TAG = "ExpandableRecyclerView";

		public static final class LongParcelable implements Parcelable {
			public static final Creator<LongParcelable> CREATOR = new Parcelable.Creator<LongParcelable>() {
				@Override
				public LongParcelable createFromParcel(Parcel in) {
					return new LongParcelable(in);
				}
				@Override
				public LongParcelable[] newArray(int size) {
					return new LongParcelable[size];
				}
			};

			private final long value;

			public LongParcelable(long value) {this.value = value;}

			private LongParcelable(Parcel in) {
				this.value = in.readLong();
			}

			@Override
			public int describeContents() {
				return 0;
			}

			@Override
			public void writeToParcel(Parcel dest, int flags) {
				dest.writeLong(value);
			}
		}

		private ExpandableRecyclerView recyclerView;

		/**
		 * Equivalent of {@link android.support.v7.widget.RecyclerView.Adapter#onCreateViewHolder(android.view.ViewGroup, int)} for an {@code ExpandableRecyclerView}.
		 * <p/>
		 * Called when ExpandableRecyclerView needs a new {@link ExpandableViewHolder} of the given type to represent
		 * an item.
		 * <p/>
		 * This new ExpandableViewHolder should be constructed with a new View that can represent the items
		 * of the given type. You can either create a new View manually or inflate it from an XML
		 * layout file.
		 * <p/>
		 * The new ExpandableViewHolder will be used to display items of the adapter using
		 * {@link #onBindGroupView(ExpandableViewHolder, int)} and child items with {@link #onBindChildView(ExpandableViewHolder, int, int)}.
		 * Since it will be re-used to display different items in the data set, it is a good idea to cache references to sub views of the View to
		 * avoid unnecessary {@link View#findViewById(int)} calls.
		 *
		 * @param parent   The ViewGroup into which the new View will be added after it is bound to
		 *                 an adapter position.
		 * @param viewType The view type of the new View.
		 * @return A new ViewHolder that holds a View of the given view type.
		 * @see #getGroupViewType(int)
		 * @see #getChildViewType(int, int)
		 * @see #onBindGroupView(ExpandableViewHolder, int)
		 * @see #onBindChildView(ExpandableViewHolder, int, int)
		 */
		@NonNull
		protected abstract VH onCreateExpandableViewHolder(ViewGroup parent, int viewType);

		/**
		 * Equivalent of {@link android.support.v7.widget.RecyclerView.Adapter#onBindViewHolder(android.support.v7.widget.RecyclerView.ViewHolder, int)} for a group View.
		 * <p/>
		 * Called by ExpandableRecyclerView to display the data at the specified position. This method
		 * should update the contents of the {@link ExpandableViewHolder#itemView} to reflect the item at
		 * the given position.
		 * <p/>
		 * Note that unlike {@link android.widget.ListView}, ExpandableRecyclerView will not call this
		 * method again if the position of the item changes in the data set unless the item itself
		 * is invalidated or the new position cannot be determined. For this reason, you should only
		 * use the <code>position</code> parameter while acquiring the related data item inside this
		 * method and should not keep a copy of it. If you need the position of an item later on
		 * (e.g. in a click listener), use {@link ExpandableRecyclerView.ExpandableViewHolder#getPosition()} which will have the
		 * updated position.
		 *
		 * @param holder        The ViewHolder which should be updated to represent the contents of the
		 *                      item at the given position in the data set.
		 * @param groupPosition The group position of the item within the adapter's data set.
		 */
		protected abstract void onBindGroupView(VH holder, int groupPosition);

		/**
		 * Equivalent of {@link android.support.v7.widget.RecyclerView.Adapter#onBindViewHolder(android.support.v7.widget.RecyclerView.ViewHolder, int)} for a child View.
		 * <p/>
		 * Called by ExpandableRecyclerView to display the data at the specified position. This method
		 * should update the contents of the {@link ExpandableViewHolder#itemView} to reflect the item at
		 * the given position.
		 * <p/>
		 * Note that unlike {@link android.widget.ListView}, ExpandableRecyclerView will not call this
		 * method again if the position of the item changes in the data set unless the item itself
		 * is invalidated or the new position cannot be determined. For this reason, you should only
		 * use the <code>position</code> parameter while acquiring the related data item inside this
		 * method and should not keep a copy of it. If you need the position of an item later on
		 * (e.g. in a click listener), use {@link ExpandableRecyclerView.ExpandableViewHolder#getPosition()} which will have the
		 * updated position.
		 *
		 * @param holder        The ViewHolder which should be updated to represent the contents of the
		 *                      item at the given position in the data set.
		 * @param groupPosition The group position of the item within the adapter's data set.
		 */
		protected abstract void onBindChildView(VH holder, int groupPosition, int childPosition);

		/**
		 * Get the amount of groups in the adapter. Similar to {@link android.widget.ExpandableListAdapter#getGroupCount() ExpandableListAdapter.getGroupCount()}.
		 */
		protected abstract int getGroupCount();

		/**
		 * Gets the number of children in a specified group. Similar to {@link android.widget.ExpandableListAdapter#getChildrenCount(int) ExpandableListAdapter.getChildrenCount()}.
		 */
		protected abstract int getChildrenCount(int groupPosition);

		/**
		 * Equivalent of {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)} for a group View.
		 * <p/>
		 * Return the view type of the item at {@code groupPosition} for the purposes
		 * of view recycling.
		 * <p/>
		 * <p>Unlike ListView adapters, types need not be contiguous. Consider using id resources to uniquely identify item view types.
		 *
		 * @param groupPosition position to query
		 * @return integer value identifying the type of the view needed to represent the item at
		 * {@code groupPosition}. Type codes need not be contiguous.
		 */
		protected abstract int getGroupViewType(int groupPosition);

		/**
		 * Equivalent of {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)} for a child View.
		 * <p/>
		 * Return the view type of the item at {@code groupPosition} for the purposes
		 * of view recycling.
		 * <p/>
		 * <p>Unlike ListView adapters, types need not be contiguous. Consider using id resources to uniquely identify item view types.
		 *
		 * @param groupPosition position to query
		 * @return integer value identifying the type of the view needed to represent the item at
		 * {@code groupPosition}. Type codes need not be contiguous.
		 */
		protected abstract int getChildViewType(int groupPosition, int childPosition);

		/**
		 * Similar to {@link android.widget.ExpandableListAdapter#getGroup(int)}.
		 * @param groupPosition
		 * @return
		 */
		public abstract T getGroup(int groupPosition);

		private int expandedPosition = RecyclerView.NO_POSITION;
		private int expandedChildCount;

		private boolean useLegacyStableIds;
		private Parcelable expandedStableId;

		@Override
		public final long getItemId(int groupPosition) {
			Parcelable stableId = getGroupStableId(groupPosition);
			if (null==stableId)
				return NO_ID;
			return stableId.hashCode();
		}

		@Override
		public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
			if (DEBUG) Log.d(LOG_TAG,  this+" onCreateViewHolder(type="+viewType+')');
			VH result = onCreateExpandableViewHolder(parent, viewType);
			return result;
		}

		@Override
		public final void onBindViewHolder(VH holder, int groupPosition) {
			if (DEBUG) Log.d(LOG_TAG,  this+" onBindViewHolder(pos="+groupPosition+") expanded="+expandedPosition+" count="+expandedChildCount);

			holder.setExpandHandler(this);

			if (expandedPosition == RecyclerView.NO_POSITION || groupPosition <= expandedPosition) {
				holder.isSelected = groupPosition == recyclerView.selectedGroup;
				onBindGroupView(holder, groupPosition);
				setExpandedViewHolder(holder, groupPosition == expandedPosition, true);
			} else if (groupPosition <= expandedPosition + expandedChildCount) {
				holder.isSelected = expandedPosition == recyclerView.selectedGroup;
				if (!BuildConfig.DEBUG) {
					try {
						onBindChildView(holder, expandedPosition, groupPosition - expandedPosition - 1);
					} catch (ClassCastException e) {
						Log.e(LOG_TAG, this + " failed onBindViewHolder(pos=" + groupPosition + ") expanded=" + expandedPosition + " count=" + expandedChildCount, e);
					}
				} else {
					onBindChildView(holder, expandedPosition, groupPosition - expandedPosition - 1);
				}
			} else {
				holder.isSelected = groupPosition == recyclerView.selectedGroup;
				onBindGroupView(holder, groupPosition - expandedChildCount);
				setExpandedViewHolder(holder, false, true);
			}
		}

		private void setExpandedViewHolder(@NonNull ExpandableViewHolder expandedViewHolder, boolean isExpanded, boolean forceUpdate) {
			if (forceUpdate || isExpanded != expandedViewHolder.isExpanded()) {
				if (DEBUG) Log.d(LOG_TAG,  this+" setExpandedViewHolder("+expandedViewHolder+")="+isExpanded);
				expandedViewHolder.expanded = isExpanded;
				expandedViewHolder.onExpandedChanged();
			}
		}

		/**
		 * No stable IDs, equivalent to calling the old {@code setHasStableIds(false)}
		 */
		public static final int STABLE_IDS_NONE = 0;
		/**
		 * Stable IDs using the old system of {@link #getGroupId(int)}
		 */
		public static final int STABLE_IDS_LONG = 1;
		/**
		 * Stables IDs using Parcelable instead of long using {@link #getGroupStableId(int)} and {@link #getGroupStableIdPosition(android.os.Parcelable)}
		 */
		public static final int STABLE_IDS_PARCELABLE = 2;

		@IntDef({STABLE_IDS_NONE, STABLE_IDS_LONG, STABLE_IDS_PARCELABLE})
		public @interface StableIdsMode {}

		/**
		 * Use {@link #setStableIdsMode(int)} instead
		 * @param hasStableIds
		 */
		@Deprecated
		@Override
		public final void setHasStableIds(boolean hasStableIds) {
			throw new IllegalAccessError("use setStableIdsMode()");
		}

		/**
		 * Specify the stable ID mode used by this adapter.
		 * This is the mode to handle the stable ID used to recover the position. It replaces {@link com.levelupstudio.recyclerview.ExpandableRecyclerView.ExpandableAdapter#hasStableIds()}
		 * by allowing a {@code Parcelable} rather than a {@code long}.
		 * <p>It can be {@link #STABLE_IDS_NONE}, {@link #STABLE_IDS_LONG} or {@link #STABLE_IDS_PARCELABLE}.</p>
		 * <p>You must override {@link #getGroupStableId(int)} and {@link #getGroupStableIdPosition(android.os.Parcelable)} for this to work properly.</p>
		 */
		public void setStableIdsMode(@StableIdsMode int stableIdsMode) {
			if (stableIdsMode == STABLE_IDS_NONE) {
				super.setHasStableIds(false);
				useLegacyStableIds = false;
			} else {
				super.setHasStableIds(true);
				useLegacyStableIds = stableIdsMode == STABLE_IDS_LONG;
			}
		}

		/**
		 * Similar to {@link android.widget.ExpandableListAdapter#getGroupId(int) ExpandableListAdapter.getGroupId()}
		 * when using {@link #STABLE_IDS_LONG} with {@link #setStableIdsMode(int)}. Otherwise {@link #getGroupStableId(int)} is used.
		 */
		protected long getGroupId(int groupPosition) {
			return NO_ID;
		}

		/**
		 * Get the stable ID at group position so the position can be recovered properly. Returns {@code null} by default.
		 * <p>Used when {@link #STABLE_IDS_PARCELABLE} is set on {@link #setStableIdsMode(int)}.</p>
		 */
		protected Parcelable getGroupStableId(int groupPosition) {
			return null;
		}

		/**
		 * Get the group position corresponding to the specified {@code stableId} to recover the position of that group.
		 * Returns {@link #NO_POSITION} by default.
		 */
		protected int getGroupStableIdPosition(Parcelable stableId) {
			return NO_POSITION;
		}

		@Override
		public final int getItemCount() {
			return getGroupCount() + (expandedPosition != RecyclerView.NO_POSITION ? expandedChildCount : 0);
		}

		@Override
		public final int getItemViewType(int groupPosition) {
			final int viewType;
			if (expandedPosition == RecyclerView.NO_POSITION || groupPosition <= expandedPosition) {
				viewType = getGroupViewType(groupPosition);
				if (viewType < 0) {
					throw new IllegalStateException("invalid viewType "+viewType+" for position "+groupPosition+" expandedPosition="+expandedPosition+" expandedChildCount="+expandedChildCount);
				}
			} else if (groupPosition - expandedPosition <= expandedChildCount) {
				viewType = getChildViewType(expandedPosition, groupPosition - expandedPosition - 1);
				if (viewType < 0) {
					throw new IllegalStateException("invalid viewType "+viewType+" for position "+groupPosition+" expandedPosition="+expandedPosition+" expandedChildCount="+expandedChildCount);
				}
			} else {
				viewType = getGroupViewType(groupPosition - expandedChildCount);
				if (viewType < 0) {
					throw new IllegalStateException("invalid viewType "+viewType+" for position "+groupPosition+" expandedPosition="+expandedPosition+" expandedChildCount="+expandedChildCount);
				}
			}
			if (DEBUG) Log.v(LOG_TAG,  this+" getItemViewType("+groupPosition+") ="+viewType);

			return viewType;
		}

		protected boolean setExpandedPosition(int expandedGroupPosition) {
			if (expandedGroupPosition >= getGroupCount()) {
				if (DEBUG) Log.d(LOG_TAG,  ExpandableAdapter.this + " the expanded position is not valid anymore expandedPosition=" + expandedGroupPosition + " groupCount=" + getGroupCount());
				expandedGroupPosition = RecyclerView.NO_POSITION;
			}

			if (this.expandedPosition != expandedGroupPosition) {
				if (DEBUG) Log.d(LOG_TAG,  this+" setExpandedPosition "+expandedGroupPosition+" from "+expandedGroupPosition+" recyclerView="+recyclerView);
				this.expandedPosition = expandedGroupPosition;
				this.expandedStableId = null;
				if (expandedGroupPosition != RecyclerView.NO_POSITION) {
					this.expandedChildCount = getChildrenCount(expandedGroupPosition);
					if (hasStableIds()) {
						if (useLegacyStableIds) {
							long expandedId = getGroupId(expandedGroupPosition);
							if (expandedId != NO_ID)
								this.expandedStableId = new LongParcelable(expandedId);
						} else
							this.expandedStableId = getGroupStableId(expandedGroupPosition);
					}
				} else {
					this.expandedChildCount = 0;
				}

				return true;
			}
			return false;
		}

		private int getHolderGroupPosition(ExpandableViewHolder holder, boolean strict) {
			int holderGroupPosition =  holder.getPosition();
			if (holderGroupPosition != RecyclerView.NO_POSITION) {
				holderGroupPosition -= getHeaderViewsCount();

				if (expandedPosition != RecyclerView.NO_POSITION) {
					if (holderGroupPosition <= expandedPosition) {
						// do nothing
					} else if (holderGroupPosition < expandedPosition + expandedChildCount) {
						// this is a child view
						if (strict) {
							throw new IndexOutOfBoundsException("expand an invalid ViewHolder holderPosition=" + holderGroupPosition + " expanded=" + expandedPosition + " count=" + expandedChildCount+" holder="+holder);
						}
					} else {
						holderGroupPosition -= expandedChildCount;
					}
				}
			}
			return holderGroupPosition;
		}

		@Override
		public final void onViewExpand(ExpandableViewHolder holder) {
			int holderGroupPosition = getHolderGroupPosition(holder, true);
			if (DEBUG) Log.w(LOG_TAG,  this+" onViewExpand groupPos="+holderGroupPosition+" holder="+holder+" recyclerView="+recyclerView);
			if (null!=recyclerView.onGroupClickListener && recyclerView.onGroupClickListener.onGroupClick(recyclerView, holder.itemView, holderGroupPosition, 0))
				return; // tap already handled

			if (holderGroupPosition == expandedPosition) {
				recyclerView.expandAndCollapse(RecyclerView.NO_POSITION, expandedPosition);
			} else {
				recyclerView.expandAndCollapse(holderGroupPosition, expandedPosition);
			}
		}

		void attachRecyclerView(ExpandableRecyclerView recyclerView) {
			if (DEBUG) Log.w(LOG_TAG,  this+" attachRecyclerView recyclerView="+recyclerView+" was "+this.recyclerView);
			this.recyclerView = recyclerView;
		}

		@Override
		public void onViewRecycled(VH holder) {
			super.onViewRecycled(holder);
			holder.setExpandHandler(null);
		}

		/**
		 * Same as {@link #notifyDataSetChanged()} but overridable.
		 */
		public void notifyDataChanged() {
			if (DEBUG) Log.i(LOG_TAG,  this+" notifyDataChanged recyclerView="+recyclerView);
			if (null != recyclerView) {
				recyclerView.stopScroll();
			}

			notifyDataSetChanged();

			if (expandedStableId != null) {
				// recover the position of the old expanded element (depends on stable IDs)
				if (!useLegacyStableIds) {
					if (DEBUG) Log.i(LOG_TAG,  this+" notifyDataChanged recovering expanded position for "+expandedStableId);
					setExpandedPosition(getGroupStableIdPosition(expandedStableId));
				}
			} else {
				setExpandedPosition(expandedPosition);
			}

			if (null != recyclerView && recyclerView.selectedStableId != null) {
				if (!useLegacyStableIds) {
					if (DEBUG) Log.i(LOG_TAG,  this+" notifyDataChanged recovering selected position for "+expandedStableId);
					recyclerView.selectedGroup = getGroupStableIdPosition(recyclerView.selectedStableId);
				}
			}
		}

		private int getHeaderViewsCount() {
			return recyclerView == null ? 0 : recyclerView.getHeaderViewsCount();
		}

		/**
		 * Notifies the item at group position changed and the display should be updated.
		 */
		public void notifyGroupChanged(int groupPosition) {
			if (null==recyclerView)
				return;

			final int modifiedStart;
			final int itemChangedCount;
			if (expandedPosition == RecyclerView.NO_POSITION || groupPosition < expandedPosition) {
				// the modified item is before the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition;
			} else if (groupPosition == expandedPosition) {
				// the modified item is the expanded item
				itemChangedCount = expandedChildCount + 1;
				modifiedStart = groupPosition;
			} else {
				// the modified item is after the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition + expandedChildCount;
			}
			if (DEBUG) Log.d(LOG_TAG,  this+" notifyGroupChanged("+groupPosition+") start="+modifiedStart+" count="+itemChangedCount+" expanded="+expandedPosition+" headerCount="+getHeaderViewsCount());
			recyclerView.changeRange(modifiedStart, itemChangedCount);
		}

		/**
		 * Notifies an item has been inserted at group position. The item insertion will be animated.
		 */
		public void notifyGroupInserted(int groupPosition) {
			if (null==recyclerView)
				return;

			final int modifiedStart;
			final int itemChangedCount;
			if (expandedPosition == RecyclerView.NO_POSITION) {
				// the inserted item is before the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition;
			} else if (groupPosition < expandedPosition) {
				// the inserted item is before the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition;
				setExpandedPosition(expandedPosition + 1);
			} else if (groupPosition == expandedPosition) {
				// the inserted item is over the expanded item
				itemChangedCount = expandedChildCount + 1;
				modifiedStart = groupPosition;
				setExpandedPosition(expandedPosition + 1);
			} else {
				// the inserted item is after the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition + expandedChildCount;
			}
			if (DEBUG) Log.d(LOG_TAG,  this+" notifyGroupInserted("+groupPosition+") start="+modifiedStart+" count="+itemChangedCount+" expanded="+expandedPosition+" headerCount="+getHeaderViewsCount());
			recyclerView.insertRange(modifiedStart, itemChangedCount);
		}

		/**
		 * Notifies the item at group position has been removed. The item removal will be animated.
		 */
		public void notifyGroupRemoved(int groupPosition) {
			if (null==recyclerView)
				return;

			final int modifiedStart;
			final int itemChangedCount;
			if (expandedPosition == RecyclerView.NO_POSITION || groupPosition < expandedPosition) {
				// the removed item is before the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition;
			} else if (groupPosition == expandedPosition) {
				// the removed item is the expanded item
				itemChangedCount = expandedChildCount + 1;
				modifiedStart = groupPosition;
				setExpandedPosition(RecyclerView.NO_POSITION);
			} else {
				// the removed item is after the expanded item
				itemChangedCount = 1;
				modifiedStart = groupPosition + expandedChildCount;
				setExpandedPosition(expandedPosition - 1);
			}
			if (DEBUG) Log.d(LOG_TAG,  this+" notifyGroupRemoved("+groupPosition+") start="+modifiedStart+" count="+itemChangedCount+" expanded="+expandedPosition+" headerCount="+getHeaderViewsCount());
			recyclerView.removeRange(modifiedStart, itemChangedCount);
		}

		/**
		 * Get the currently expanded element or {@code null} if no item is expanded.
		 */
		public @Nullable T getExpandedGroup() {
			if (expandedPosition == RecyclerView.NO_POSITION)
				return null;

			return getGroup(expandedPosition);
		}
	}

	public ExpandableRecyclerView(Context context) {
		super(context);
		init();
	}

	public ExpandableRecyclerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ExpandableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mNormalItemAnimator = super.getItemAnimator();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!firstLayoutPassed) {
			if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, this+" first layout");
			firstLayoutPassed = true;
		}
		super.onLayout(changed, l, t, r, b);
	}

	public boolean isFirstLayoutPassed() {
		return firstLayoutPassed;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		firstLayoutPassed = false;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		firstLayoutPassed = false;
	}

	/**
	 * Use {@link #setExpandableAdapter(com.levelupstudio.recyclerview.ExpandableRecyclerView.ExpandableAdapter)}.
	 */
	@Deprecated
	@Override
	public void setAdapter(Adapter adapter) {
		if (adapter != null && !(adapter instanceof ExpandableAdapter))
			throw new IllegalStateException("use a ExpandableAdapter not "+adapter);
		setExpandableAdapter((ExpandableAdapter) adapter);
	}

	public void setExpandableAdapter(ExpandableAdapter adapter) {
		if (getAdapter() instanceof ExpandableAdapter) {
			ExpandableAdapter expandableAdapter = (ExpandableAdapter) getAdapter();
			expandableAdapter.attachRecyclerView(null);
		}

		super.setAdapter(adapter);
		if (null!=adapter)
			adapter.attachRecyclerView(this);
	}

	public ExpandableAdapter getExpandableAdapter() {
		return (ExpandableAdapter) getAdapter();
	}

	/**
	 * Get the {@link android.support.v7.widget.RecyclerView.ItemAnimator} used to handle animations other than the collapse/expand.
	 */
	@Override
	public ItemAnimator getItemAnimator() {
		return mNormalItemAnimator;
	}

	/**
	 * Set the {@link android.support.v7.widget.RecyclerView.ItemAnimator} used to handle animations other than the collapse/expand.
	 * @param animator
	 */
	@Override
	public void setItemAnimator(final ItemAnimator animator) {
		if (super.getItemAnimator() == mNormalItemAnimator) {
			super.setItemAnimator(animator);
		}
		mNormalItemAnimator = animator;
	}

	private void expandAndCollapse(final int expandPosition, final int collapsePosition) {
		if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, "expandAndCollapse "+expandPosition+'/'+collapsePosition+" currentAnimator="+super.getItemAnimator());

		if (super.getItemAnimator() != null) {
			// wait until that ItemAnimator has finished processing its queue to go on with ours

			super.getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
				@Override
				public void onAnimationsFinished() {
					ExpandableRecyclerView.super.setItemAnimator(new ExpandAndCollapseItemAnimator(expandPosition, collapsePosition));
					doExpandAndCollapse(expandPosition, collapsePosition);
				}
			});
			return;
		}

		doExpandAndCollapse(expandPosition, collapsePosition);
	}

	private void doExpandAndCollapse(final int expandPosition, final int collapsePosition) {
		ExpandableAdapter expandableAdapter = getExpandableAdapter();
		boolean collapseChanged = expandableAdapter.expandedPosition != RecyclerView.NO_POSITION && collapsePosition == expandableAdapter.expandedPosition;
		// collapse
		if (collapseChanged) {
			if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, "collapse group " + collapsePosition + " in "+getExpandableAdapter());
			getAdapter().notifyItemRangeRemoved(collapsePosition + getHeaderViewsCount() + 1, expandableAdapter.getChildrenCount(collapsePosition));
			expandableAdapter.setExpandedPosition(RecyclerView.NO_POSITION);
		}

		// expand
		boolean expandedChanged = expandableAdapter.setExpandedPosition(expandPosition);
		if (expandedChanged) {
			int childViewCount = expandableAdapter.getChildrenCount(expandPosition);
			getAdapter().notifyItemRangeInserted(expandPosition + getHeaderViewsCount() + 1, childViewCount);
			if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, "expand group " + expandPosition + " in "+getExpandableAdapter());
		}

		if (collapseChanged || expandedChanged) {
			boolean expandedIsShown = false;
			if (expandPosition != RecyclerView.NO_POSITION) {
				if (getLayoutManager() instanceof LinearLayoutManager) {
					LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
					//Log.e(LOG_TAG,  "lastFull = "+linearLayoutManager.findLastCompletelyVisibleItemPosition()+" group+Child"+(groupPosition + childViewCount));
					if (linearLayoutManager.findFirstVisibleItemPosition() < expandPosition + getHeaderViewsCount() &&
							linearLayoutManager.findLastCompletelyVisibleItemPosition() < expandPosition + getHeaderViewsCount()) {
						expandedIsShown = true;
						if (ExpandableAdapter.DEBUG) Log.e(ExpandableAdapter.LOG_TAG, "doExpandAndCollapse() the expandedIsShown");
					}
				}
			}

			// dirty trick to scroll to show the last visible expandable item when it's not visible and do some other stuff
			ViewCompat.postOnAnimationDelayed(this, new Runnable() {
				@Override
				public void run() {
					ExpandableRecyclerView.super.getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
						@Override
						public void onAnimationsFinished() {
							if (getExpandableAdapter()==null)
								return;

							if (collapsePosition != RecyclerView.NO_POSITION) {
								ExpandableViewHolder viewHolder = (ExpandableViewHolder) findViewHolderForPosition(collapsePosition + getHeaderViewsCount());
								if (null != viewHolder) {
									getExpandableAdapter().setExpandedViewHolder(viewHolder, false, false);
								}
							}

							if (expandPosition != RecyclerView.NO_POSITION) {
								ExpandableViewHolder viewHolder = (ExpandableViewHolder) findViewHolderForPosition(expandPosition + getHeaderViewsCount());
								if (null != viewHolder) {
									getExpandableAdapter().setExpandedViewHolder(viewHolder, true, false);
								}

								if (getLayoutManager() instanceof LinearLayoutManager) {
									LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
									int childViewCount = getExpandableAdapter().getChildrenCount(expandPosition);
									//Log.e(LOG_TAG,  "lastFull = "+linearLayoutManager.findLastCompletelyVisibleItemPosition()+" group+Child"+(groupPosition + childViewCount));
									if (linearLayoutManager.findFirstVisibleItemPosition() < expandPosition + getHeaderViewsCount() &&
											linearLayoutManager.findLastCompletelyVisibleItemPosition() < expandPosition + getHeaderViewsCount() + childViewCount) {
										if (ExpandableAdapter.DEBUG) Log.i(ExpandableAdapter.LOG_TAG, "scroll to show more expanded items");

										smoothScrollToPosition(expandPosition + getHeaderViewsCount() + childViewCount);
									}
								}
							}

							ExpandableRecyclerView.super.setItemAnimator(mNormalItemAnimator);
						}
					});
				}
			}, ExpandableRecyclerView.super.getItemAnimator().getMoveDuration());
		}
	}

	/**
	 * Force expanding of the specified group.
	 */
	public void expandGroup(final int groupPosition) {
		if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, "expandGroup "+groupPosition);
		expandAndCollapse(groupPosition, RecyclerView.NO_POSITION);
	}

	/**
	 * Force collapsing of the specified group.
	 */
	public void collapseGroup(int groupPosition) {
		if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, "collapseGroup "+groupPosition);
		expandAndCollapse(RecyclerView.NO_POSITION, groupPosition);
	}

	private void doSetSelectedGroup(int groupPosition) {
		if (groupPosition < 0)
			groupPosition = RecyclerView.NO_POSITION;

		if (groupPosition != selectedGroup) {
			if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG,  "doSetSelectedGroup("+groupPosition+") selectedGroup="+selectedGroup);
			if (selectedGroup != RecyclerView.NO_POSITION) {
				ExpandableViewHolder selectedViewHolder = (ExpandableViewHolder) findViewHolderForPosition(selectedGroup);
				if (null != selectedViewHolder) {
					selectedViewHolder.isSelected = false;
					getExpandableAdapter().notifyGroupChanged(selectedGroup);
				}
			}

			selectedGroup = groupPosition;
			if (getExpandableAdapter().hasStableIds()) {
				if (getExpandableAdapter().useLegacyStableIds)
					selectedStableId = new ExpandableAdapter.LongParcelable(getExpandableAdapter().getGroupId(groupPosition));
				else
					selectedStableId = getExpandableAdapter().getGroupStableId(groupPosition);
			} else {
				selectedStableId = null;
			}

			if (selectedGroup != RecyclerView.NO_POSITION) {
				ExpandableViewHolder selectedViewHolder = (ExpandableViewHolder) findViewHolderForPosition(selectedGroup);
				if (null != selectedViewHolder) {
					selectedViewHolder.isSelected = true;
					getExpandableAdapter().notifyGroupChanged(selectedGroup);
				}
			}
		}
	}

	/**
	 * Equivalent of {@link android.widget.ExpandableListView#setSelectedGroup(int)} for an {@code ExpandableRecyclerView}.
	 */
	public void setSelectedGroup(final int groupPosition) {
		if (ExpandableAdapter.DEBUG) Log.v(ExpandableAdapter.LOG_TAG, "setSelectedGroup("+groupPosition+')');
		if (super.getItemAnimator() != null) {
			super.getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
				@Override
				public void onAnimationsFinished() {
					doSetSelectedGroup(groupPosition);
				}
			});
			return;
		}

		doSetSelectedGroup(groupPosition);
	}

	/**
	 * Equivalent of {@link android.widget.ExpandableListView#getSelectedPosition()} for an {@code ExpandableRecyclerView}.
	 */
	public long getSelectedPosition() {
		if (selectedGroup == RecyclerView.NO_POSITION)
			return ExpandableListView.PACKED_POSITION_VALUE_NULL;
		return ExpandableListView.getPackedPositionForGroup(selectedGroup);
	}

	public void collapseAll() {
		if (ExpandableAdapter.DEBUG) Log.d(ExpandableAdapter.LOG_TAG, "collapseAll");
		expandAndCollapse(RecyclerView.NO_POSITION, getExpandableAdapter().expandedPosition);
	}

	/**
	 * Equivalent of {@link android.widget.ExpandableListView#setOnGroupExpandListener(android.widget.ExpandableListView.OnGroupExpandListener)} for an {@code ExpandableRecyclerView}.
	 */
	public void setOnGroupExpandListener(ExpandableListView.OnGroupExpandListener onGroupExpandListener) {
		this.onGroupExpandListener = onGroupExpandListener;
	}

	/**
	 * Equivalent of {@link android.widget.ExpandableListView#setOnGroupCollapseListener(android.widget.ExpandableListView.OnGroupCollapseListener)} for an {@code ExpandableRecyclerView}.
	 */
	public void setOnGroupCollapseListener(ExpandableListView.OnGroupCollapseListener onGroupCollapseListener) {
		this.onGroupCollapseListener = onGroupCollapseListener;
	}

	/**
	 * Equivalent of {@link android.widget.ExpandableListView#setOnGroupClickListener(android.widget.ExpandableListView.OnGroupClickListener)} for an {@code ExpandableRecyclerView}.
	 */
	public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener) {
		this.onGroupClickListener = onGroupClickListener;
	}

	private void changeRange(final int groupPosition, final int childCount) {
		if (super.getItemAnimator() == mNormalItemAnimator) {
			getAdapter().notifyItemRangeChanged(groupPosition + getHeaderViewsCount(), childCount);
		} else if (super.getItemAnimator() == null) {
			super.setItemAnimator(mNormalItemAnimator);
			getAdapter().notifyItemRangeChanged(groupPosition + getHeaderViewsCount(), childCount);
		} else {
			super.getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
				@Override
				public void onAnimationsFinished() {
					if (ExpandableRecyclerView.super.getItemAnimator() != mNormalItemAnimator) {
						ExpandableRecyclerView.super.setItemAnimator(mNormalItemAnimator);
					}
					changeRange(groupPosition, childCount);
				}
			});
		}
	}

	private void insertRange(final int groupPosition, final int childCount) {
		if (super.getItemAnimator() == mNormalItemAnimator) {
			getAdapter().notifyItemRangeInserted(groupPosition + getHeaderViewsCount(), childCount);
		} else if (super.getItemAnimator() == null) {
			super.setItemAnimator(mNormalItemAnimator);
			getAdapter().notifyItemRangeInserted(groupPosition + getHeaderViewsCount(), childCount);
		} else {
			super.getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
				@Override
				public void onAnimationsFinished() {
					if (ExpandableRecyclerView.super.getItemAnimator() != mNormalItemAnimator) {
						ExpandableRecyclerView.super.setItemAnimator(mNormalItemAnimator);
					}
					insertRange(groupPosition, childCount);
				}
			});
		}
	}

	private void removeRange(final int groupPosition, final int childCount) {
		if (super.getItemAnimator() == mNormalItemAnimator) {
			getAdapter().notifyItemRangeRemoved(groupPosition + getHeaderViewsCount(), childCount);
		} else if (super.getItemAnimator() == null) {
			super.setItemAnimator(mNormalItemAnimator);
			getAdapter().notifyItemRangeRemoved(groupPosition + getHeaderViewsCount(), childCount);
		} else {
			super.getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
				@Override
				public void onAnimationsFinished() {
					if (ExpandableRecyclerView.super.getItemAnimator() != mNormalItemAnimator) {
						ExpandableRecyclerView.super.setItemAnimator(mNormalItemAnimator);
					}
					removeRange(groupPosition, childCount);
				}
			});
		}
	}

	private final Runnable refreshDisplay = new Runnable() {
		@Override
		public void run() {
			if (getLayoutManager() instanceof LinearLayoutManager && isFirstLayoutPassed() && getExpandableAdapter()!=null) {
				LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
				int first = layoutManager.findFirstVisibleItemPosition();
				int last = layoutManager.findLastVisibleItemPosition();
				getAdapter().notifyItemRangeChanged(first, last);
			}
		}
	};

	/**
	 * Refresh all the displayed items (rebind the data to update the content)
	 */
	public void refreshDisplay() {
		if (getItemAnimator() == null) {
			removeCallbacks(refreshDisplay);
			post(refreshDisplay);
		} else {
			getItemAnimator().isRunning(new ItemAnimator.ItemAnimatorFinishedListener() {
				@Override
				public void onAnimationsFinished() {
					removeCallbacks(refreshDisplay);
					post(refreshDisplay);
				}
			});
		}
	}

	private class ExpandAndCollapseItemAnimator extends DefaultItemAnimator {
		private final int expandPosition;
		private final int collapsePosition;
		private boolean expandListenerCalled;
		private boolean collapseListenerCalled;

		public ExpandAndCollapseItemAnimator(int expandPosition, int collapsePosition) {
			this.expandPosition = expandPosition;
			this.collapsePosition = collapsePosition;

			collapseListenerCalled = collapsePosition == RecyclerView.NO_POSITION;
			expandListenerCalled = expandPosition == RecyclerView.NO_POSITION;

			setAddDuration(0);
			setRemoveDuration(0);
			setMoveDuration(200);
		}

		@Override
		public void onRemoveFinished(ViewHolder item) {
			super.onRemoveFinished(item);

			// TODO even when the element was not shown

			if (!collapseListenerCalled && item instanceof ExpandableViewHolder) {
				int holderPosition = getExpandableAdapter().getHolderGroupPosition((ExpandableViewHolder) item, false);
				if (holderPosition == collapsePosition) {
					if (ExpandableAdapter.DEBUG) Log.v(ExpandableAdapter.LOG_TAG, "removed the collapsed item");
					if (null != onGroupCollapseListener)
						onGroupCollapseListener.onGroupCollapse(collapsePosition);
					collapseListenerCalled = true;
				}
			}
		}
		@Override
		public void onAddFinished(ViewHolder item) {
			super.onAddFinished(item);

			// TODO even when the element was not shown

			if (!expandListenerCalled && item instanceof ExpandableViewHolder) {
				int holderPosition = getExpandableAdapter().getHolderGroupPosition((ExpandableViewHolder) item, false);
				if (holderPosition == expandPosition) {
					if (ExpandableAdapter.DEBUG) Log.v(ExpandableAdapter.LOG_TAG, "added the expanded item");
					if (null != onGroupExpandListener)
						onGroupExpandListener.onGroupExpand(expandPosition);
					expandListenerCalled = true;
				}
			}
		}
	}

	static class SavedState extends AbsSavedState {

		public Parcelable selectedStableId;
		public Parcelable expandedStableId;

		public SavedState(Parcel in) {
			super(in.readParcelable(RecyclerView.class.getClassLoader()));
			selectedStableId = in.readParcelable(getClass().getClassLoader());
			expandedStableId = in.readParcelable(getClass().getClassLoader());
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@Override
		public void writeToParcel(@NonNull Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeParcelable(selectedStableId, 0);
			dest.writeParcelable(expandedStableId, 0);
		}

		public static final Parcelable.Creator<SavedState> CREATOR
				= new Parcelable.Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		SavedState state = new SavedState(super.onSaveInstanceState());
		state.selectedStableId = this.selectedStableId;
		ExpandableAdapter adapter = getExpandableAdapter();
		state.expandedStableId = adapter.expandedStableId;
		return state;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		this.selectedStableId = savedState.selectedStableId;
		ExpandableAdapter adapter = getExpandableAdapter();
		if (null != adapter && adapter.hasStableIds()) {
			adapter.expandedStableId = savedState.expandedStableId;
		}
	}
}
